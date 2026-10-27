// The Nomad rules engine lives in web/src/engine, shared with the website. Create React App only
// compiles files under src/, so this copies the engine in before every start and build.
const fs = require("fs");
const path = require("path");

const from = path.join(__dirname, "..", "..", "web", "src", "engine");
const to = path.join(__dirname, "..", "src", "nomad", "engine");
fs.rmSync(to, { recursive: true, force: true });
fs.mkdirSync(to, { recursive: true });
for (const name of fs.readdirSync(from)) {
    if (!name.endsWith(".ts") || name.endsWith(".test.ts")) continue;
    const header = "// Copied from web/src/engine by scripts/copy-engine.js. Edit the original.\n";
    fs.writeFileSync(path.join(to, name), header + fs.readFileSync(path.join(from, name), "utf8"));
}
console.log("Copied the Nomad engine into src/nomad/engine");
