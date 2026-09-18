import { copyFile, mkdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const output = new URL("../target/project-site/", import.meta.url);

await mkdir(new URL("assets/", output), { recursive: true });
for (const filename of ["index.html", "styles.css"]) {
  await copyFile(new URL(filename, import.meta.url), new URL(filename, output));
}
await copyFile(
  new URL("../docs/images/qip-investigation-workspace.png", import.meta.url),
  new URL("assets/investigation.png", output),
);
console.log(`Static site built at ${fileURLToPath(output)}`);
