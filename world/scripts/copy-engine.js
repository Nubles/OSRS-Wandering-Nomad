// The Nomad rules engine lives in web/src/engine, shared with the website. Create React App only
// compiles files under src/, so this copies the engine in before every start and build, along
// with the two data files it reads: world.dat (land and kingdoms) and heights.bin (the ground).
const fs = require("fs");
const path = require("path");

const web = path.join(__dirname, "..", "..", "web");
const from = path.join(web, "src", "engine");
const to = path.join(__dirname, "..", "src", "nomad", "engine");
fs.rmSync(to, { recursive: true, force: true });
fs.mkdirSync(to, { recursive: true });
for (const name of fs.readdirSync(from)) {
    if (!name.endsWith(".ts") || name.endsWith(".test.ts")) continue;
    const header = "// Copied from web/src/engine by scripts/copy-engine.js. Edit the original.\n";
    fs.writeFileSync(path.join(to, name), header + fs.readFileSync(path.join(from, name), "utf8"));
}

const data = path.join(__dirname, "..", "public", "nomad");
fs.mkdirSync(data, { recursive: true });
// world.dat is the plugin's own copy; the website copies it the same way when it builds.
const worldDat = path.join(web, "..", "src", "main", "resources", "com", "wanderingnomad", "sim", "world.dat");
fs.copyFileSync(worldDat, path.join(data, "world.dat"));
fs.copyFileSync(path.join(web, "public", "heights.bin"), path.join(data, "heights.bin"));
for (const name of ["heightBounds.json", "mapBounds.json"]) {
    fs.copyFileSync(path.join(web, "src", name), path.join(to, name));
}
console.log("Copied the Nomad engine and its data into src/nomad/engine and public/nomad");
