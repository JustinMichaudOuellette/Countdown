// Generates app/src/main/assets/emoji/emoji_data.tsv from Unicode's emoji-test.txt.
//
// Usage:
//   curl -L -o build/emoji-src/emoji-test.txt https://unicode.org/Public/emoji/16.0/emoji-test.txt
//   node tools/emoji/generate_emoji_data.js
//
// The output groups emoji into families (skin tones, tone combinations and gendered versions) and
// records, for every emoji, the first Android API level whose bundled font can draw it. Flags never
// carry variants: territories that are drawn with their sovereign state's flag are left out, so each
// flag design appears exactly once.
const fs = require("fs");

const SRC = "build/emoji-src/emoji-test.txt";
const OUT = "app/src/main/assets/emoji/emoji_data.tsv";
const MAX_VARIANTS = 30;

const GROUP_TO_CATEGORY = {
  "Smileys & Emotion": "people",
  "People & Body": "people",
  "Animals & Nature": "animals",
  "Food & Drink": "food",
  "Travel & Places": "travel",
  Activities: "activities",
  Objects: "objects",
  Symbols: "symbols",
  Flags: "flags",
};
const CATEGORY_ORDER = [
  "people",
  "animals",
  "food",
  "travel",
  "activities",
  "objects",
  "symbols",
  "flags",
];

const TONES = [
  "light skin tone",
  "medium-light skin tone",
  "medium skin tone",
  "medium-dark skin tone",
  "dark skin tone",
];

const GENDER_ONLY_SEGMENT = /^(woman|women|man|men|girl|girls|boy|boys)$/i;

// First Android API level whose bundled emoji font can render each emoji version.
function minApiForVersion(version) {
  const value = parseFloat(version);
  if (value >= 16.0) return 36;
  if (value >= 15.1) return 35;
  if (value >= 15.0) return 34;
  if (value >= 14.0) return 33;
  if (value >= 13.1) return 31;
  if (value >= 13.0) return 30;
  if (value >= 12.1) return 30;
  if (value >= 12.0) return 29;
  if (value >= 11.0) return 28;
  return 26;
}
const GENDERED_WORDS = /\b(woman|women|man|men|girl|girls|boy|boys)\b/i;

// "man scientist", "woman scientist" and "scientist" all end up under the same merge key.
function mergeKeyFor(familyKey) {
  return familyKey
    .replace(/\b(women|woman|men|man)\b/gi, "person")
    .replace(/^(person|people)\s+/i, "")
    .replace(/\s+/g, " ")
    .trim();
}

function isNeutralFamilyKey(familyKey) {
  return !GENDERED_WORDS.test(familyKey);
}

// Splits "person: light skin tone, red hair" into a family key plus tone info.
function analyze(name) {
  const colon = name.indexOf(":");
  const base = (colon < 0 ? name : name.slice(0, colon)).trim();
  const extras =
    colon < 0
      ? []
      : name
          .slice(colon + 1)
          .split(",")
          .map((s) => s.trim());
  const tones = [];
  const descriptors = [];
  for (const extra of extras) {
    if (!extra) continue;
    if (extra.endsWith("skin tone")) tones.push(extra);
    else if (!GENDER_ONLY_SEGMENT.test(extra)) descriptors.push(extra);
  }
  const familyKey = descriptors.length ? `${base}: ${descriptors.join(", ")}` : base;
  let toneRank = 0;
  for (const tone of tones) {
    const idx = TONES.indexOf(tone);
    toneRank = toneRank * 6 + (idx < 0 ? 0 : idx + 1);
  }
  return { familyKey, toneRank, toneCount: tones.length };
}

const lineRe = /^([0-9A-F ]+?)\s*;\s*(\S+)\s*#\s*(\S+)\s+E(\d+\.\d+)\s+(.*)$/;

// Some territories are drawn with their sovereign state's flag, so Android renders them exactly
// like another flag. Flags do not offer variants, so keeping them would make the picker look like it
// shows the same flag twice; they are left out and each flag design appears exactly once.
const SkippedDuplicateFlags = new Map([
  ["\u{1F1FA}\u{1F1F2}", "the United States flag"], // U.S. Outlying Islands
  ["\u{1F1E7}\u{1F1FB}", "the Norway flag"], // Bouvet Island
  ["\u{1F1F8}\u{1F1EF}", "the Norway flag"], // Svalbard & Jan Mayen
  ["\u{1F1ED}\u{1F1F2}", "the Australia flag"], // Heard & McDonald Islands
  ["\u{1F1E8}\u{1F1F5}", "the France flag"], // Clipperton Island
  ["\u{1F1F9}\u{1F1EB}", "the France flag"], // French Southern Territories
  ["\u{1F1F2}\u{1F1EB}", "the France flag"], // Saint Martin
  ["\u{1F1EA}\u{1F1E6}", "the Spain flag"], // Ceuta & Melilla
]);

const records = [];
let group = "";
let subgroup = "";
let order = 0;
const skippedFlagsSeen = new Set();

for (const raw of fs.readFileSync(SRC, "utf8").split("\n")) {
  const line = raw.trimEnd();
  if (line.startsWith("# group:")) {
    group = line.slice("# group:".length).trim();
    continue;
  }
  if (line.startsWith("# subgroup:")) {
    subgroup = line.slice("# subgroup:".length).trim();
    continue;
  }
  if (!line || line.startsWith("#")) continue;
  const m = lineRe.exec(line);
  if (!m) throw new Error(`Unparsed line: ${line}`);
  if (m[2] !== "fully-qualified") continue;
  const category = GROUP_TO_CATEGORY[group];
  if (!category) continue; // Component group
  if (SkippedDuplicateFlags.has(m[3])) {
    skippedFlagsSeen.add(m[3]);
    continue;
  }
  const name = m[5].trim();
  const { familyKey, toneRank, toneCount } = analyze(name);
  records.push({
    emoji: m[3],
    name,
    group,
    subgroup,
    category,
    familyKey,
    toneRank,
    toneCount,
    version: m[4],
    minApi: minApiForVersion(m[4]),
    order: order++,
  });
}

// Fail loudly if Unicode renames or drops one of the skipped flags, so the list cannot go stale.
for (const [flag, description] of SkippedDuplicateFlags) {
  if (!skippedFlagsSeen.has(flag)) {
    throw new Error(`Skipped flag ${flag} (${description}) is no longer in emoji-test.txt`);
  }
}

// Group into families (same group + same family key).
const families = new Map();
for (const record of records) {
  const key = `${record.group}|${record.familyKey}`;
  let family = families.get(key);
  if (!family) {
    family = {
      key,
      group: record.group,
      familyKey: record.familyKey,
      mergeKey: mergeKeyFor(record.familyKey),
      neutral: isNeutralFamilyKey(record.familyKey),
      canonical: record.name === record.familyKey,
      category: record.category,
      members: [],
    };
    families.set(key, family);
  }
  if (record.name === record.familyKey) family.canonical = true;
  family.members.push(record);
}

// Merge gendered families (man / woman ...) into the neutral family they belong to, so that the
// grid shows one cell per concept and every gender and skin tone stays selectable as a variant.
const byScope = new Map();
for (const family of families.values()) {
  const scope = `${family.group}|${family.mergeKey}`;
  if (!byScope.has(scope)) byScope.set(scope, []);
  byScope.get(scope).push(family);
}
const hiddenFamilies = new Set();
for (const scopeFamilies of byScope.values()) {
  if (scopeFamilies.length < 2) continue;
  const target = scopeFamilies.find((f) => f.neutral);
  if (!target) continue; // no neutral form: keep the families separate
  for (const family of scopeFamilies) {
    if (family === target) continue;
    hiddenFamilies.add(family);
    for (const member of family.members) target.members.push(member);
  }
}

function sortMembers(members) {
  return members.sort((a, b) => {
    if (a.toneCount !== b.toneCount) return a.toneCount - b.toneCount;
    return a.order - b.order;
  });
}

function pickBase(members) {
  const sameTone = (m) => {
    const tones = m.name
      .slice(m.name.indexOf(":") + 1)
      .split(",")
      .map((s) => s.trim())
      .filter((s) => s.endsWith("skin tone"));
    return new Set(tones).size <= 1;
  };
  return (
    members.find((m) => m.name === m.familyKey) ??
    members.find((m) => m.toneCount === 0) ??
    members.find(sameTone) ??
    members[0]
  );
}

const entriesByCategory = new Map(CATEGORY_ORDER.map((c) => [c, []]));
const variantsByEmoji = new Map();

for (const family of families.values()) {
  if (hiddenFamilies.has(family)) continue;
  const members = sortMembers(family.members);
  const base = pickBase(members);
  const position = Math.min(...members.map((m) => m.order));
  let variants = [base, ...members.filter((m) => m !== base)].map((m) => m.emoji);
  if (variants.length > MAX_VARIANTS) variants = variants.slice(0, MAX_VARIANTS);
  for (const member of members) {
    variantsByEmoji.set(
      member.emoji,
      variants.includes(member.emoji) ? variants : [member.emoji, ...variants.slice(0, MAX_VARIANTS - 1)]
    );
  }
  entriesByCategory.get(family.category).push({ emoji: base.emoji, variants, position });
}

for (const category of CATEGORY_ORDER) {
  entriesByCategory.get(category).sort((a, b) => a.position - b.position);
}

const lines = [];
lines.push("# Emoji dataset for the Countdown emoji picker.");
lines.push("# Derived from Unicode emoji-test.txt v16.0, https://www.unicode.org/terms_of_use.html");
lines.push("# Regenerate with tools/emoji/generate_emoji_data.js");
lines.push("# Format: C<TAB>category, then <emoji>:<first supported API><TAB><space separated variants>");
const minApiByEmoji = new Map(records.map((record) => [record.emoji, record.minApi]));
const token = (emoji) => `${emoji}:${minApiByEmoji.get(emoji) ?? 26}`;
for (const category of CATEGORY_ORDER) {
  lines.push(`C\t${category}`);
  for (const entry of entriesByCategory.get(category)) {
    lines.push(
      entry.variants.length > 1
        ? `${token(entry.emoji)}\t${entry.variants.map(token).join(" ")}`
        : token(entry.emoji)
    );
  }
}

fs.mkdirSync("app/src/main/assets/emoji", { recursive: true });
fs.writeFileSync(OUT, lines.join("\n") + "\n", "utf8");

// --- report ---
const stats = CATEGORY_ORDER.map((c) => {
  const entries = entriesByCategory.get(c);
  const variants = entries.reduce((n, e) => n + e.variants.length, 0);
  return `${c}: ${entries.length} cells, ${variants} total emoji`;
});
console.log(stats.join("\n"));
console.log(`records: ${records.length}, families: ${families.size}, hidden: ${hiddenFamilies.size}`);
console.log(`file: ${(fs.statSync(OUT).size / 1024).toFixed(1)} KB`);

function printFamily(family) {
  const members = sortMembers([...family.members]);
  const base = pickBase(members);
  console.log(
    `  [${family.group}] ${family.familyKey} base=${base.emoji} (${members.length}) -> ${[
      base,
      ...members.filter((m) => m !== base),
    ]
      .slice(0, 8)
      .map((m) => m.emoji)
      .join(" ")}`
  );
}
console.log("samples:");
for (const [key, family] of families) {
  if (hiddenFamilies.has(family)) continue;
  if (
    /^(handshake|waving hand|couple with heart|people holding hands|family|keycap: 1|blond-haired person|person shrugging)/.test(
      family.familyKey
    )
  ) {
    printFamily(family);
  }
}
const emojiChecks = ["👶", "🦋", "👩‍❤️‍👨", "💑", "👋"];
for (const e of emojiChecks) {
  const variants = variantsByEmoji.get(e);
  console.log(`  suggested ${e} -> ${variants ? variants.slice(0, 10).join(" ") + (variants.length > 10 ? " ..." : "") : "MISSING"}`);
}
