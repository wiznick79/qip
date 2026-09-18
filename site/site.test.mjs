import assert from "node:assert/strict";
import { readFile, stat, readdir } from "node:fs/promises";
import test from "node:test";

const root = new URL("../target/project-site/", import.meta.url);
const html = await readFile(new URL("index.html", root), "utf8");

test("local links and fragments resolve under a Pages project path", async () => {
  const ids = [...html.matchAll(/\bid="([^"]+)"/g)].map((match) => match[1]);
  assert.equal(new Set(ids).size, ids.length, "duplicate IDs");
  for (const [, target] of html.matchAll(/(?:href|src)="([^"]+)"/g)) {
    if (target.startsWith("https://")) {
      assert.equal(
        new URL(target).hostname,
        "github.com",
        "unexpected external dependency",
      );
    } else if (target.startsWith("#")) {
      assert.ok(
        target === "#" || ids.includes(target.slice(1)),
        `missing anchor ${target}`,
      );
    } else {
      assert.ok(
        !target.startsWith("/"),
        "root-relative paths break project Pages hosting",
      );
      assert.ok(
        (await stat(new URL(target, root))).isFile(),
        `missing asset ${target}`,
      );
    }
  }
});

test("the tour works without JavaScript or a live backend", () => {
  assert.doesNotMatch(html, /<script|<iframe|<form|\son\w+=/i);
  for (const id of ["workflow", "architecture", "start"]) {
    assert.ok(html.includes(`id="${id}"`));
  }
  assert.match(html, /insufficient evidence/i);
  assert.match(html, /synthetic/i);
  assert.equal([...html.matchAll(/<h1\b/g)].length, 1);
  assert.equal([...html.matchAll(/<details\b/g)].length, 4);
  for (const [, attributes] of html.matchAll(/<img\b([^>]+)>/g)) {
    assert.match(attributes, /alt="[^"]+"/);
  }
});

test("publication artifact contains only the intended public files", async () => {
  assert.deepEqual((await readdir(root)).sort(), [
    "assets",
    "index.html",
    "styles.css",
  ]);
  assert.deepEqual(await readdir(new URL("assets/", root)), [
    "investigation.png",
  ]);
  const css = await readFile(new URL("styles.css", root), "utf8");
  assert.doesNotMatch(css, /@import|https?:\/\//i);
});
