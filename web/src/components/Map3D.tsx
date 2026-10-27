import { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import type { Tile } from '../engine/geography';
import { VOLCANO } from '../engine/geography';
import { contains, MAIN, touches } from '../engine/sim';
import { MAP } from '../mapBounds';
import { loadHeights, loadMapImage } from '../useGeography';
import type { MapScene } from './MapCanvas';

interface Props {
  scene: React.RefObject<MapScene | null>;
  onPick?: (tile: Tile) => void;
  className?: string;
}

/** Hills are real heights from the game, stretched so they read from a distance. */
const EXAGGERATE = 2.5;
const WALL_SEGMENTS = 96;
const RING_POINTS = 128;
const MAX_CIRCLES = 16;

const W = MAP.x1 - MAP.x0;
const H = MAP.yTop - MAP.yBottom;
const MID_X = MAP.x0 + W / 2;
const MID_Y = MAP.yBottom + H / 2;

/** World tiles to scene space: x east, z south, y up, one unit per tile. */
const sx = (x: number) => x - MID_X;
const sz = (y: number) => MID_Y - y;

const WALL_VERTEX = /* glsl */ `
  varying float vUp;
  void main() {
    vUp = uv.y;
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
  }
`;
const WALL_FRAGMENT = /* glsl */ `
  uniform vec3 uColour;
  uniform float uStrength;
  uniform float uTime;
  varying float vUp;
  void main() {
    float fade = pow(1.0 - vUp, 2.2);
    float shimmer = 0.85 + 0.15 * sin(vUp * 30.0 - uTime * 2.0);
    gl_FragColor = vec4(uColour, fade * uStrength * shimmer);
  }
`;

export function Map3D({ scene, onPick, className }: Props) {
  const hostRef = useRef<HTMLDivElement>(null);
  const labelsRef = useRef<HTMLDivElement>(null);
  const followRef = useRef<() => void>(() => undefined);
  const zoomRef = useRef<(f: number) => void>(() => undefined);
  const onPickRef = useRef(onPick);
  onPickRef.current = onPick;
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const host = hostRef.current!;
    const labelHost = labelsRef.current!;
    let renderer: THREE.WebGLRenderer;
    try {
      renderer = new THREE.WebGLRenderer({ antialias: true });
    } catch {
      setFailed(true);
      return;
    }
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    host.prepend(renderer.domElement);
    renderer.domElement.setAttribute('aria-label', '3D map of Gielinor with the Nomad circles');
    renderer.domElement.setAttribute('role', 'img');

    const world = new THREE.Scene();
    world.background = new THREE.Color('#1d2a3a');
    world.fog = new THREE.Fog('#1d2a3a', 900, 2600);
    const camera = new THREE.PerspectiveCamera(45, 1, 2, 6000);
    world.add(new THREE.HemisphereLight('#fff6e0', '#2a3040', 1.1));
    const sun = new THREE.DirectionalLight('#ffffff', 1.6);
    sun.position.set(-1, 1.4, -0.6);
    world.add(sun);

    // Terrain: a grid with the game's ground heights, wearing the map.
    let heightAt = (_x: number, _y: number) => 0;
    const fogUniforms = {
      uYours: { value: new THREE.Vector3(0, 0, 0) },
      uFog: { value: 1 },
    };
    const groundMat = new THREE.MeshLambertMaterial({ color: '#ffffff' });
    groundMat.onBeforeCompile = (shader) => {
      Object.assign(shader.uniforms, fogUniforms);
      shader.vertexShader = shader.vertexShader
        .replace('#include <common>', '#include <common>\nvarying vec2 vGround;')
        .replace('#include <begin_vertex>', '#include <begin_vertex>\nvGround = position.xz;');
      shader.fragmentShader = shader.fragmentShader
        .replace('#include <common>', '#include <common>\nvarying vec2 vGround;\nuniform vec3 uYours;\nuniform float uFog;')
        .replace(
          '#include <dithering_fragment>',
          `#include <dithering_fragment>
          float d = distance(vGround, uYours.xy) - uYours.z;
          gl_FragColor.rgb *= mix(1.0, 0.42, uFog * smoothstep(0.0, 3.0, d));`,
        );
    };
    let ground: THREE.Mesh | null = null;
    let disposed = false;
    Promise.all([loadHeights(), loadMapImage()]).then(([hm, img]) => {
      if (disposed) return;
      const geo = new THREE.PlaneGeometry(W, H, hm.cols - 1, hm.rows - 1);
      geo.rotateX(-Math.PI / 2);
      const pos = geo.attributes.position as THREE.BufferAttribute;
      const unit = (hm.scale / 128) * EXAGGERATE;
      for (let i = 0; i < pos.count; i++) pos.setY(i, hm.data[i] * unit);
      geo.computeVertexNormals();
      heightAt = (x, y) => {
        const fx = ((x - MAP.x0) / W) * (hm.cols - 1);
        const fy = ((MAP.yTop - y) / H) * (hm.rows - 1);
        const c = Math.max(0, Math.min(hm.cols - 2, Math.floor(fx)));
        const r = Math.max(0, Math.min(hm.rows - 2, Math.floor(fy)));
        const tx = Math.max(0, Math.min(1, fx - c));
        const ty = Math.max(0, Math.min(1, fy - r));
        const at = (cc: number, rr: number) => hm.data[rr * hm.cols + cc];
        const top = at(c, r) * (1 - tx) + at(c + 1, r) * tx;
        const bottom = at(c, r + 1) * (1 - tx) + at(c + 1, r + 1) * tx;
        return (top * (1 - ty) + bottom * ty) * unit;
      };
      const tex = new THREE.Texture(img);
      tex.colorSpace = THREE.SRGBColorSpace;
      tex.anisotropy = renderer.capabilities.getMaxAnisotropy();
      tex.needsUpdate = true;
      groundMat.map = tex;
      groundMat.needsUpdate = true;
      ground = new THREE.Mesh(geo, groundMat);
      world.add(ground);
    }, () => setFailed(true));

    // One wall and one ground ring per circle, made on first sight.
    interface CircleMeshes {
      wall: THREE.Mesh;
      wallMat: THREE.ShaderMaterial;
      ring: THREE.LineLoop;
      ringMat: THREE.LineBasicMaterial;
      label: HTMLDivElement;
      voyage: THREE.Line;
    }
    const wallGeo = new THREE.CylinderGeometry(1, 1, 1, WALL_SEGMENTS, 1, true);
    wallGeo.translate(0, 0.5, 0);
    const meshes: CircleMeshes[] = [];
    const meshFor = (i: number): CircleMeshes => {
      if (meshes[i]) return meshes[i];
      const wallMat = new THREE.ShaderMaterial({
        uniforms: { uColour: { value: new THREE.Color() }, uStrength: { value: 0.5 }, uTime: { value: 0 } },
        vertexShader: WALL_VERTEX,
        fragmentShader: WALL_FRAGMENT,
        transparent: true,
        depthWrite: false,
        side: THREE.DoubleSide,
        blending: THREE.AdditiveBlending,
      });
      const wall = new THREE.Mesh(wallGeo, wallMat);
      const ringGeo = new THREE.BufferGeometry();
      ringGeo.setAttribute('position', new THREE.BufferAttribute(new Float32Array(RING_POINTS * 3), 3));
      const ringMat = new THREE.LineBasicMaterial({ transparent: true });
      const ring = new THREE.LineLoop(ringGeo, ringMat);
      const voyageGeo = new THREE.BufferGeometry();
      voyageGeo.setAttribute('position', new THREE.BufferAttribute(new Float32Array(6), 3));
      const voyage = new THREE.Line(voyageGeo, new THREE.LineDashedMaterial({ dashSize: 8, gapSize: 6, transparent: true }));
      const label = document.createElement('div');
      label.className = 'map3d-label';
      labelHost.appendChild(label);
      world.add(wall, ring, voyage);
      meshes[i] = { wall, wallMat, ring, ringMat, label, voyage };
      return meshes[i];
    };

    const player = new THREE.Group();
    {
      const pin = new THREE.Mesh(new THREE.ConeGeometry(2.2, 7, 16), new THREE.MeshLambertMaterial({ color: '#ffffff' }));
      pin.rotateX(Math.PI);
      pin.position.y = 3.5;
      player.add(pin);
    }
    world.add(player);
    const volcano = new THREE.Mesh(new THREE.ConeGeometry(9, 14, 20), new THREE.MeshLambertMaterial({ color: '#ff5a36', emissive: '#5a1000' }));
    world.add(volcano);

    // Camera: orbit around your circle, tilted like a bird's-eye view.
    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.08;
    controls.minDistance = 40;
    controls.maxDistance = 2600;
    controls.maxPolarAngle = Math.PI * 0.46;
    controls.screenSpacePanning = false;
    controls.addEventListener('start', () => {
      if (scene.current) scene.current.follow = false;
    });
    let placed = false;
    followRef.current = () => {
      if (scene.current) scene.current.follow = true;
    };
    zoomRef.current = (f: number) => {
      const off = camera.position.clone().sub(controls.target).multiplyScalar(1 / f);
      const len = Math.min(controls.maxDistance, Math.max(controls.minDistance, off.length()));
      camera.position.copy(controls.target).add(off.setLength(len));
    };

    const resize = () => {
      const w = host.clientWidth;
      const h = host.clientHeight;
      if (w === 0 || h === 0) return;
      renderer.setSize(w, h, false);
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
    };
    const ro = new ResizeObserver(resize);
    ro.observe(host);
    resize();

    const colourOf = new THREE.Color();
    const v = new THREE.Vector3();
    let frame = 0;
    const draw = (t: number) => {
      frame = requestAnimationFrame(draw);
      const s = scene.current;
      if (!s) return;
      const sim = s.sim;
      const yours = sim.bound;
      const yoursGround = heightAt(yours.x, yours.y);

      if (!placed) {
        controls.target.set(sx(yours.x), yoursGround, sz(yours.y));
        camera.position.set(sx(yours.x) - 60, yoursGround + 230, sz(yours.y) + 230);
        placed = true;
      }
      if (s.follow) {
        const to = new THREE.Vector3(sx(yours.x), yoursGround, sz(yours.y));
        const step = to.sub(controls.target).multiplyScalar(0.08);
        controls.target.add(step);
        camera.position.add(step);
      }
      controls.update();

      fogUniforms.uYours.value.set(sx(yours.x), sz(yours.y), yours.radius);
      fogUniforms.uFog.value = s.fog ? 1 : 0;

      const w = host.clientWidth;
      const h = host.clientHeight;
      sim.circles.slice(0, MAX_CIRCLES).forEach((c, i) => {
        const m = meshFor(i);
        const isYours = i === sim.state.bound;
        const crossable = sim.linked(i) && touches(yours, c) && !isYours;
        colourOf.setHex(sim.colour(c));
        const tall = i === MAIN ? 70 : isYours ? 55 : 30;
        m.wall.position.set(sx(c.x), 0, sz(c.y));
        m.wall.scale.set(c.radius, tall + (c.pulse > 0 ? (c.pulse / 45) * 25 : 0), c.radius);
        m.wallMat.uniforms.uColour.value.copy(colourOf);
        m.wallMat.uniforms.uStrength.value = isYours ? 0.75 : crossable ? 0.6 : i === MAIN ? 0.5 : 0.28;
        m.wallMat.uniforms.uTime.value = t / 1000;
        m.wall.renderOrder = 2;

        const arr = (m.ring.geometry.attributes.position as THREE.BufferAttribute).array as Float32Array;
        for (let k = 0; k < RING_POINTS; k++) {
          const a = (k / RING_POINTS) * Math.PI * 2;
          const x = c.x + Math.cos(a) * c.radius;
          const y = c.y + Math.sin(a) * c.radius;
          arr[k * 3] = sx(x);
          arr[k * 3 + 1] = heightAt(x, y) + 0.6;
          arr[k * 3 + 2] = sz(y);
        }
        m.ring.geometry.attributes.position.needsUpdate = true;
        m.ring.geometry.computeBoundingSphere();
        m.ringMat.color.copy(colourOf);
        m.ringMat.opacity = isYours || i === MAIN ? 1 : 0.7;

        m.voyage.visible = !!c.voyage;
        if (c.voyage) {
          const va = (m.voyage.geometry.attributes.position as THREE.BufferAttribute).array as Float32Array;
          va.set([sx(c.x), 2, sz(c.y), sx(c.voyage.tx), 2, sz(c.voyage.ty)]);
          m.voyage.geometry.attributes.position.needsUpdate = true;
          m.voyage.geometry.computeBoundingSphere();
          m.voyage.computeLineDistances();
          (m.voyage.material as THREE.LineDashedMaterial).color.copy(colourOf);
        }

        // Labels float above the wall, drawn by the page in the game font.
        v.set(sx(c.x), heightAt(c.x, c.y) + tall * 0.55, sz(c.y)).project(camera);
        const visible = v.z < 1 && Math.abs(v.x) < 1.1 && Math.abs(v.y) < 1.1;
        const distance = camera.position.distanceTo(new THREE.Vector3(sx(c.x), 0, sz(c.y)));
        const show = visible && (i === MAIN || isYours || distance < 900);
        m.label.style.display = show ? '' : 'none';
        if (show) {
          m.label.textContent = sim.name(c) + (isYours ? ' (you)' : '');
          m.label.style.color = `#${colourOf.getHexString()}`;
          m.label.style.transform = `translate(-50%, -50%) translate(${((v.x + 1) / 2) * w}px, ${((1 - v.y) / 2) * h}px)`;
          m.label.classList.toggle('is-bold', i === MAIN || isYours);
        }
      });

      player.visible = !!s.player;
      if (s.player) {
        player.position.set(sx(s.player.x), heightAt(s.player.x, s.player.y), sz(s.player.y));
        // Keep the pin a readable size however far out the camera is.
        player.scale.setScalar(Math.max(1, camera.position.distanceTo(player.position) / 120));
        const inside = contains(yours, s.player.x, s.player.y);
        ((player.children[0] as THREE.Mesh).material as THREE.MeshLambertMaterial).color.set(inside ? '#ffffff' : '#ff5252');
      }
      volcano.position.set(sx(VOLCANO.x), heightAt(VOLCANO.x, VOLCANO.y) + 7, sz(VOLCANO.y));

      renderer.render(world, camera);
    };
    frame = requestAnimationFrame(draw);

    // A tap (not a drag) stands you on that spot.
    const ray = new THREE.Raycaster();
    let downAt: { x: number; y: number } | null = null;
    const down = (e: PointerEvent) => (downAt = { x: e.clientX, y: e.clientY });
    const up = (e: PointerEvent) => {
      const start = downAt;
      downAt = null;
      if (!start || !ground || !onPickRef.current) return;
      if (Math.abs(e.clientX - start.x) + Math.abs(e.clientY - start.y) > 5) return;
      const b = renderer.domElement.getBoundingClientRect();
      ray.setFromCamera(new THREE.Vector2(((e.clientX - b.left) / b.width) * 2 - 1, -((e.clientY - b.top) / b.height) * 2 + 1), camera);
      const hit = ray.intersectObject(ground)[0];
      if (hit) onPickRef.current({ x: Math.round(hit.point.x + MID_X), y: Math.round(MID_Y - hit.point.z) });
    };
    renderer.domElement.addEventListener('pointerdown', down);
    renderer.domElement.addEventListener('pointerup', up);

    return () => {
      disposed = true;
      cancelAnimationFrame(frame);
      ro.disconnect();
      controls.dispose();
      renderer.domElement.removeEventListener('pointerdown', down);
      renderer.domElement.removeEventListener('pointerup', up);
      world.traverse((o) => {
        const mesh = o as THREE.Mesh;
        mesh.geometry?.dispose();
        const mat = mesh.material as THREE.Material | THREE.Material[] | undefined;
        (Array.isArray(mat) ? mat : mat ? [mat] : []).forEach((mm) => mm.dispose());
      });
      groundMat.map?.dispose();
      renderer.dispose();
      renderer.domElement.remove();
      labelHost.replaceChildren();
    };
  }, [scene]);

  if (failed) {
    return (
      <div className={`map-canvas map3d ${className ?? ''}`}>
        <div className="osrs-chat notice notice-error map3d-failed">Your browser can't show the 3D map. Switch back to 2D.</div>
      </div>
    );
  }

  return (
    <div className={`map-canvas map3d ${className ?? ''}`} ref={hostRef}>
      <div className="map3d-labels" ref={labelsRef} aria-hidden="true" />
      <div className="map-zoom">
        <button type="button" className="btn" aria-label="Zoom in" onClick={() => zoomRef.current(1.4)}>+</button>
        <button type="button" className="btn" aria-label="Zoom out" onClick={() => zoomRef.current(1 / 1.4)}>−</button>
        <button type="button" className="btn" aria-label="Follow your circle" title="Follow your circle" onClick={() => followRef.current()}>
          ◎
        </button>
      </div>
    </div>
  );
}

export default Map3D;
