/**
 * Land and kingdoms from world.dat, read the same way as World.java: a gzipped file with land in
 * cells of a few tiles and one kingdom byte per 64-tile map chunk.
 */
export class World {
  readonly cell: number;
  readonly x0: number;
  readonly y0: number;
  readonly cols: number;
  readonly rows: number;
  private readonly landBits: Uint8Array;
  private readonly chunkX0: number;
  private readonly chunkY0: number;
  private readonly chunkCols: number;
  private readonly chunkRows: number;
  private readonly kingdoms: Uint8Array;

  /** Parses the unzipped bytes. */
  constructor(bytes: Uint8Array) {
    const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    const magic = String.fromCharCode(bytes[0], bytes[1], bytes[2], bytes[3]);
    if (magic !== 'NMW1') throw new Error('world.dat has the wrong header');
    let at = 4;
    const int = () => {
      const v = view.getInt32(at);
      at += 4;
      return v;
    };
    this.cell = int();
    this.x0 = int();
    this.y0 = int();
    this.cols = int();
    this.rows = int();
    const landLen = (this.cols * this.rows + 7) >> 3;
    this.landBits = bytes.slice(at, at + landLen);
    at += landLen;
    this.chunkX0 = int();
    this.chunkY0 = int();
    this.chunkCols = int();
    this.chunkRows = int();
    this.kingdoms = bytes.slice(at, at + this.chunkCols * this.chunkRows);
  }

  static async fromGzip(data: ArrayBuffer): Promise<World> {
    const stream = new Blob([data]).stream().pipeThrough(new DecompressionStream('gzip'));
    return new World(new Uint8Array(await new Response(stream).arrayBuffer()));
  }

  land(x: number, y: number): boolean {
    const col = Math.floor((x - this.x0) / this.cell);
    const row = Math.floor((y - this.y0) / this.cell);
    if (col < 0 || row < 0 || col >= this.cols || row >= this.rows) return false;
    const bit = row * this.cols + col;
    return (this.landBits[bit >> 3] & (0x80 >> (bit & 7))) !== 0;
  }

  /** The kingdom index of the chunk holding a tile, or -1. */
  kingdom(x: number, y: number): number {
    const cx = Math.floor(x / 64) - this.chunkX0;
    const cy = Math.floor(y / 64) - this.chunkY0;
    if (cx < 0 || cy < 0 || cx >= this.chunkCols || cy >= this.chunkRows) return -1;
    return this.kingdoms[cy * this.chunkCols + cx] - 1;
  }
}
