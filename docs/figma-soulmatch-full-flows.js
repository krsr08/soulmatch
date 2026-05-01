// SoulMatch full matrimony mobile flow
// Prepared for the Figma MCP `use_figma` tool once the Starter-plan rate limit clears.
// Target file: https://www.figma.com/design/k90TGfMzLbK3nONJCLHclw

const availableFonts = await figma.listAvailableFontsAsync();

function pickFont(families, styleHints) {
  const normalized = availableFonts.map((entry) => ({
    family: entry.fontName.family,
    style: entry.fontName.style,
    fontName: entry.fontName,
  }));
  for (const family of families) {
    const familyFonts = normalized.filter((item) => item.family === family);
    if (!familyFonts.length) continue;
    for (const hint of styleHints) {
      const exact = familyFonts.find((item) => item.style.toLowerCase() === hint);
      if (exact) return exact.fontName;
      const partial = familyFonts.find((item) => item.style.toLowerCase().includes(hint));
      if (partial) return partial.fontName;
    }
    return familyFonts[0].fontName;
  }
  return normalized[0].fontName;
}

const headingBold = pickFont(["Poppins", "Plus Jakarta Sans", "DM Sans", "Inter"], ["bold", "semi", "medium"]);
const headingRegular = pickFont(["Poppins", "Plus Jakarta Sans", "DM Sans", "Inter"], ["medium", "regular"]);
const bodyRegular = pickFont(["Inter", "DM Sans", "Roboto", "Poppins"], ["regular", "book"]);
const bodyMedium = pickFont(["Inter", "DM Sans", "Roboto", "Poppins"], ["medium", "semi", "bold"]);
const bodyBold = pickFont(["Inter", "DM Sans", "Roboto", "Poppins"], ["bold", "semi", "medium"]);

const uniqueFonts = new Map();
[headingBold, headingRegular, bodyRegular, bodyMedium, bodyBold].forEach((font) => {
  uniqueFonts.set(`${font.family}__${font.style}`, font);
});
await Promise.all(Array.from(uniqueFonts.values()).map((font) => figma.loadFontAsync(font)));

function rgb(hex) {
  const normalized = hex.replace("#", "");
  const value = normalized.length === 3 ? normalized.split("").map((c) => c + c).join("") : normalized;
  const number = parseInt(value, 16);
  return {
    r: ((number >> 16) & 255) / 255,
    g: ((number >> 8) & 255) / 255,
    b: (number & 255) / 255,
  };
}

function solid(hex, opacity = 1) {
  return [{ type: "SOLID", color: rgb(hex), opacity }];
}

function shadow(color = "#D5B7AF", opacity = 0.14, y = 18, radius = 28) {
  return [{
    type: "DROP_SHADOW",
    color: { ...rgb(color), a: opacity },
    offset: { x: 0, y },
    radius,
    visible: true,
    blendMode: "NORMAL",
  }];
}

const c = {
  canvas: "#FCF6F1",
  surface: "#FFFFFF",
  surfaceSoft: "#FBEDE6",
  surfaceWarm: "#FFF3E8",
  primary: "#E76F6C",
  primaryDeep: "#C85358",
  peach: "#F3B489",
  gold: "#E4BF77",
  mint: "#DFF4EB",
  emerald: "#218764",
  lilac: "#F1E9FB",
  lilacText: "#7B5CA7",
  ink: "#1F2937",
  slate: "#667085",
  muted: "#98A2B3",
  line: "#EBDCD4",
  chip: "#F7ECE6",
  danger: "#D14B5A",
  dark: "#101828",
};

function rect(parent, x, y, width, height, fill, radius = 8, stroke) {
  const node = figma.createRectangle();
  node.x = x;
  node.y = y;
  node.resize(width, height);
  node.cornerRadius = radius;
  node.fills = solid(fill);
  if (stroke) {
    node.strokes = solid(stroke);
    node.strokeWeight = 1;
  } else {
    node.strokes = [];
  }
  parent.appendChild(node);
  return node;
}

function ellipse(parent, x, y, width, height, fill, opacity = 1) {
  const node = figma.createEllipse();
  node.x = x;
  node.y = y;
  node.resize(width, height);
  node.fills = solid(fill, opacity);
  node.strokes = [];
  parent.appendChild(node);
  return node;
}

function text(parent, characters, options = {}) {
  const node = figma.createText();
  node.fontName = options.font || bodyRegular;
  node.fontSize = options.size || 16;
  node.characters = characters;
  node.fills = solid(options.fill || c.ink);
  if (options.lineHeight) node.lineHeight = { unit: "PIXELS", value: options.lineHeight };
  if (options.letterSpacing) node.letterSpacing = { unit: "PIXELS", value: options.letterSpacing };
  if (options.wrapWidth) {
    node.resize(options.wrapWidth, node.height);
    node.textAutoResize = "HEIGHT";
  } else {
    node.textAutoResize = "WIDTH_AND_HEIGHT";
  }
  node.x = options.x || 0;
  node.y = options.y || 0;
  parent.appendChild(node);
  return node;
}

function measure(characters, options = {}) {
  const node = figma.createText();
  node.fontName = options.font || bodyRegular;
  node.fontSize = options.size || 16;
  node.characters = characters;
  node.textAutoResize = "WIDTH_AND_HEIGHT";
  const result = { width: node.width, height: node.height };
  node.remove();
  return result;
}

function pill(parent, x, y, label, options = {}) {
  const font = options.font || bodyMedium;
  const size = options.size || 12;
  const padX = options.padX || 12;
  const padY = options.padY || 8;
  const metrics = measure(label, { font, size });
  const width = metrics.width + padX * 2 + (options.dot ? 16 : 0);
  const height = Math.max(30, metrics.height + padY * 2);
  rect(parent, x, y, width, height, options.fill || c.surface, 999, options.stroke);
  if (options.dot) {
    ellipse(parent, x + 10, y + (height - 8) / 2, 8, 8, options.dotColor || c.primary);
  }
  text(parent, label, {
    x: x + padX + (options.dot ? 10 : 0),
    y: y + (height - metrics.height) / 2,
    font,
    size,
    fill: options.text || c.ink,
  });
}

function button(parent, x, y, width, height, label, options = {}) {
  const background = rect(parent, x, y, width, height, options.fill || c.primary, options.radius || 16, options.stroke);
  if (options.shadow) background.effects = shadow("#D6A39F", 0.18, 10, 18);
  const labelNode = text(parent, label, {
    font: bodyBold,
    size: 15,
    fill: options.text || "#FFFFFF",
  });
  labelNode.x = x + (width - labelNode.width) / 2;
  labelNode.y = y + (height - labelNode.height) / 2;
}

function avatar(parent, x, y, size, fill, label) {
  ellipse(parent, x, y, size, size, fill);
  const labelNode = text(parent, label, {
    font: headingBold,
    size: Math.round(size * 0.32),
    fill: "#FFFFFF",
  });
  labelNode.x = x + (size - labelNode.width) / 2;
  labelNode.y = y + (size - labelNode.height) / 2 - 1;
}

function field(parent, x, y, width, label, value, options = {}) {
  text(parent, label, { x, y, font: bodyBold, size: 12, fill: c.slate });
  rect(parent, x, y + 20, width, options.height || 48, options.fill || "#FFFDFC", 14, c.line);
  text(parent, value, {
    x: x + 16,
    y: y + 35,
    font: options.valueFont || bodyMedium,
    size: 14,
    fill: options.text || c.ink,
    wrapWidth: width - 32,
    lineHeight: options.lineHeight || 20,
  });
}

function card(parent, x, y, width, height, fill = c.surface) {
  const node = rect(parent, x, y, width, height, fill, 24, c.line);
  node.effects = shadow("#D6B9AF", 0.1, 10, 20);
  return node;
}

function appBar(screen, titleValue, subtitleValue, avatarLabel) {
  text(screen, titleValue, { x: 24, y: 52, font: headingBold, size: 26, fill: c.ink });
  if (subtitleValue) {
    text(screen, subtitleValue, { x: 24, y: 86, font: bodyRegular, size: 14, fill: c.slate });
  }
  if (avatarLabel) {
    avatar(screen, 344, 46, 44, c.primary, avatarLabel);
  }
}

function bottomNav(screen, active) {
  const base = rect(screen, 0, 836, 412, 81, "#FFFDFC", 0);
  base.strokes = [];
  rect(screen, 0, 836, 412, 1, c.line, 0);
  ["Home", "Search", "Activity", "Chat", "Profile"].forEach((label, index) => {
    const x = 14 + index * 78;
    if (index === active) {
      rect(screen, x, 848, 66, 40, c.surfaceSoft, 20);
      ellipse(screen, x + 10, 858, 18, 18, c.primary);
      ellipse(screen, x + 16, 864, 6, 6, "#FFFFFF");
      text(screen, label, { x: x + 32, y: 861, font: bodyMedium, size: 11, fill: c.primaryDeep });
    } else {
      ellipse(screen, x + 12, 860, 14, 14, c.muted, 0.4);
      text(screen, label, { x: x + 2, y: 880, font: bodyRegular, size: 11, fill: c.slate });
    }
  });
}

function searchBar(screen, y, placeholder) {
  rect(screen, 24, y, 280, 48, "#FFFDFC", 16, c.line);
  text(screen, placeholder, { x: 42, y: y + 16, font: bodyRegular, size: 14, fill: c.muted });
  button(screen, 316, y, 72, 48, "Filter", { fill: c.surface, text: c.ink, stroke: c.line, radius: 16 });
}

function phone(parent, x, y, name) {
  const frame = figma.createFrame();
  frame.name = name;
  frame.x = x;
  frame.y = y;
  frame.resize(412, 917);
  frame.cornerRadius = 34;
  frame.clipsContent = true;
  frame.fills = solid(c.canvas);
  frame.strokes = solid(c.line);
  frame.strokeWeight = 1;
  frame.effects = shadow("#D1B7AE", 0.16, 24, 38);
  parent.appendChild(frame);
  text(frame, "9:41", { x: 24, y: 14, font: bodyMedium, size: 12, fill: c.ink });
  rect(frame, 338, 18, 18, 8, c.ink, 4);
  rect(frame, 360, 18, 14, 8, c.ink, 4);
  rect(frame, 378, 18, 12, 8, c.ink, 4);
  return frame;
}

function boardLabel(board, x, y, titleValue) {
  text(board, titleValue, { x, y, font: bodyBold, size: 16, fill: c.ink });
}

function screenSetupHub(screen) {
  appBar(screen, "Build your matrimonial profile", "Everything important, step by step", "S");
  card(screen, 24, 126, 364, 136, c.surfaceSoft);
  text(screen, "Profile readiness", { x: 42, y: 150, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "2 of 6 sections complete", { x: 42, y: 178, font: headingBold, size: 24, fill: c.primaryDeep });
  rect(screen, 42, 220, 286, 10, c.surface, 999);
  rect(screen, 42, 220, 120, 10, c.primary, 999);
  pill(screen, 290, 172, "34%", { fill: c.surface, text: c.primaryDeep, stroke: c.line });
  [
    ["Basic details", "8 required fields", "Done"],
    ["Physical and career", "9 details", "Continue"],
    ["Family and lifestyle", "10 details", "Continue"],
    ["Horoscope and preferences", "8 details", "Pending"],
    ["Photos and privacy", "6 actions", "Pending"],
  ].forEach((item, index) => {
    const y = 286 + index * 96;
    card(screen, 24, y, 364, 80, index === 0 ? c.surfaceSoft : c.surface);
    text(screen, item[0], { x: 42, y: y + 18, font: bodyBold, size: 15, fill: c.ink });
    text(screen, item[1], { x: 42, y: y + 44, font: bodyRegular, size: 13, fill: c.slate });
    pill(screen, 288, y + 24, item[2], {
      fill: item[2] === "Done" ? c.mint : c.surfaceWarm,
      text: item[2] === "Done" ? c.emerald : c.primaryDeep,
    });
  });
  button(screen, 24, 778, 364, 56, "Continue profile", { fill: c.primary, shadow: true });
}

function screenBasic(screen) {
  appBar(screen, "Basic details", "These fields drive the first layer of matching");
  field(screen, 24, 130, 170, "First name", "Aarya");
  field(screen, 218, 130, 170, "Last name", "Sharma");
  field(screen, 24, 208, 170, "Date of birth", "1997-09-14");
  field(screen, 218, 208, 170, "Gender", "Female");
  field(screen, 24, 286, 170, "Religion", "Hindu");
  field(screen, 218, 286, 170, "Community / caste", "Brahmin");
  field(screen, 24, 364, 170, "Mother tongue", "Hindi");
  field(screen, 218, 364, 170, "Marital status", "Never married");
  card(screen, 24, 456, 364, 132, c.surfaceWarm);
  text(screen, "Why ask this now", { x: 42, y: 480, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "Other matrimony apps ask these identity fields upfront because they affect match quality, family expectations, and search accuracy.", {
    x: 42,
    y: 510,
    font: bodyRegular,
    size: 14,
    fill: c.slate,
    wrapWidth: 300,
    lineHeight: 22,
  });
  button(screen, 24, 778, 174, 56, "Save step", { fill: c.surface, text: c.ink, stroke: c.line, radius: 16 });
  button(screen, 214, 778, 174, 56, "Continue", { fill: c.primary, shadow: true });
}

function screenCareer(screen) {
  appBar(screen, "Physical and career", "Structured fields keep filtering simple and fast");
  field(screen, 24, 130, 110, "Height cm", "168");
  field(screen, 151, 130, 110, "Weight kg", "58");
  field(screen, 278, 130, 110, "Blood group", "B+");
  field(screen, 24, 208, 170, "Complexion", "Wheatish");
  field(screen, 218, 208, 170, "Body type", "Athletic");
  field(screen, 24, 286, 364, "Education", "MBA / Post Graduate");
  field(screen, 24, 364, 364, "Occupation", "Product manager");
  field(screen, 24, 442, 170, "Annual income", "20+ LPA");
  field(screen, 218, 442, 170, "Working city", "Bangalore");
  card(screen, 24, 532, 364, 182, c.surface);
  text(screen, "Filter impact", { x: 42, y: 556, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "These fields should directly power search filters, shortlist ranking, and partner preference matching. They should never feel buried later in the flow.", {
    x: 42,
    y: 586,
    font: bodyRegular,
    size: 14,
    fill: c.slate,
    wrapWidth: 300,
    lineHeight: 22,
  });
  pill(screen, 42, 654, "Education filter", { fill: c.surfaceSoft, text: c.primaryDeep });
  pill(screen, 170, 654, "Income band", { fill: c.mint, text: c.emerald });
  pill(screen, 42, 694, "City search", { fill: c.lilac, text: c.lilacText });
  button(screen, 24, 778, 174, 56, "Back", { fill: c.surface, text: c.ink, stroke: c.line, radius: 16 });
  button(screen, 214, 778, 174, 56, "Continue", { fill: c.primary, shadow: true });
}

function screenFamily(screen) {
  appBar(screen, "Family and lifestyle", "This is where the profile starts to feel real");
  field(screen, 24, 130, 170, "Father occupation", "Retired banker");
  field(screen, 218, 130, 170, "Mother occupation", "Teacher");
  field(screen, 24, 208, 80, "Brothers", "1");
  field(screen, 120, 208, 80, "Sisters", "0");
  field(screen, 216, 208, 172, "Family type", "Nuclear");
  field(screen, 24, 286, 364, "Family city", "Lucknow");
  field(screen, 24, 364, 110, "Diet", "Vegetarian");
  field(screen, 151, 364, 110, "Smoking", "Never");
  field(screen, 278, 364, 110, "Drinking", "Socially");
  field(screen, 24, 442, 364, "About me", "Calm, family-first, curious, and serious about building a warm life with the right partner.", {
    height: 98,
    lineHeight: 21,
  });
  card(screen, 24, 566, 364, 148, c.surfaceWarm);
  text(screen, "Standout UX move", { x: 42, y: 590, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "Prompt users with examples here so the About section becomes more specific than generic matrimony bios.", {
    x: 42,
    y: 620,
    font: bodyRegular,
    size: 14,
    fill: c.slate,
    wrapWidth: 300,
    lineHeight: 22,
  });
  pill(screen, 42, 674, "Family rhythm", { fill: c.surface, text: c.primaryDeep, stroke: c.line });
  pill(screen, 168, 674, "Future city", { fill: c.surface, text: c.primaryDeep, stroke: c.line });
  button(screen, 24, 778, 174, 56, "Back", { fill: c.surface, text: c.ink, stroke: c.line, radius: 16 });
  button(screen, 214, 778, 174, 56, "Continue", { fill: c.primary, shadow: true });
}

function screenHoroscope(screen) {
  appBar(screen, "Horoscope and preferences", "Optional for some users, essential for others");
  field(screen, 24, 130, 170, "Rashi", "Tula");
  field(screen, 218, 130, 170, "Nakshatra", "Swati");
  field(screen, 24, 208, 170, "Manglik", "No");
  field(screen, 218, 208, 170, "Birth city", "Lucknow");
  field(screen, 24, 286, 364, "Gotra", "Bharadwaj");
  card(screen, 24, 380, 364, 240, c.surface);
  text(screen, "Partner preferences", { x: 42, y: 404, font: bodyBold, size: 15, fill: c.ink });
  field(screen, 42, 432, 128, "Age min", "27");
  field(screen, 186, 432, 128, "Age max", "33");
  field(screen, 42, 510, 272, "Preferred religion", "Hindu");
  field(screen, 42, 588, 272, "Manglik pref", "Any");
  text(screen, "These preferences should directly sync with Smart Search and home ranking.", {
    x: 42,
    y: 664,
    font: bodyRegular,
    size: 13,
    fill: c.slate,
    wrapWidth: 290,
    lineHeight: 20,
  });
  button(screen, 24, 778, 174, 56, "Back", { fill: c.surface, text: c.ink, stroke: c.line, radius: 16 });
  button(screen, 214, 778, 174, 56, "Continue", { fill: c.primary, shadow: true });
}

function screenPhotos(screen) {
  appBar(screen, "Photos, privacy, verification", "Trust signals should be visible before matching starts");
  card(screen, 24, 126, 364, 194, c.surface);
  text(screen, "Profile media", { x: 42, y: 150, font: bodyBold, size: 14, fill: c.ink });
  rect(screen, 42, 184, 112, 112, c.surfaceSoft, 18, c.line);
  rect(screen, 166, 184, 112, 112, c.surfaceWarm, 18, c.line);
  rect(screen, 290, 184, 80, 112, c.chip, 18, c.line);
  text(screen, "+ video", { x: 304, y: 230, font: bodyBold, size: 14, fill: c.primaryDeep });
  pill(screen, 42, 278, "Primary photo", { fill: c.mint, text: c.emerald });
  card(screen, 24, 346, 364, 206, c.surfaceWarm);
  text(screen, "Privacy and verification", { x: 42, y: 370, font: bodyBold, size: 14, fill: c.ink });
  field(screen, 42, 398, 304, "Photo privacy", "Visible to shortlist + mutual interest");
  field(screen, 42, 476, 304, "Profile visibility", "Visible in search");
  pill(screen, 42, 526, "OTP verified", { fill: c.mint, text: c.emerald });
  pill(screen, 156, 526, "ID pending", { fill: c.surface, text: c.primaryDeep, stroke: c.line });
  pill(screen, 260, 526, "Selfie check", { fill: c.surface, text: c.primaryDeep, stroke: c.line });
  card(screen, 24, 578, 364, 126, c.surface);
  text(screen, "Why this stands out", { x: 42, y: 602, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "Top matrimony apps surface privacy and verification clearly. SoulMatch should do this earlier and more elegantly.", {
    x: 42,
    y: 632,
    font: bodyRegular,
    size: 14,
    fill: c.slate,
    wrapWidth: 300,
    lineHeight: 22,
  });
  button(screen, 24, 778, 174, 56, "Back", { fill: c.surface, text: c.ink, stroke: c.line, radius: 16 });
  button(screen, 214, 778, 174, 56, "Publish profile", { fill: c.primary, shadow: true });
}

function screenHome(screen) {
  appBar(screen, "Your best matches", "High-intent, verified, easier to act on", "A");
  searchBar(screen, 126, "Search by city, community, profession");
  pill(screen, 24, 188, "Verified", { fill: c.surfaceSoft, text: c.primaryDeep });
  pill(screen, 108, 188, "High compatibility", { fill: c.mint, text: c.emerald });
  pill(screen, 258, 188, "Recently active", { fill: c.lilac, text: c.lilacText });
  card(screen, 24, 232, 364, 430, c.surface);
  rect(screen, 24, 232, 364, 212, c.surfaceSoft, 24, c.line);
  ellipse(screen, 144, 264, 124, 124, c.primary);
  pill(screen, 42, 252, "97% match", { fill: c.surface, text: c.primaryDeep, stroke: c.line });
  pill(screen, 300, 252, "ID", { fill: c.mint, text: c.emerald });
  text(screen, "Rhea Singh, 29", { x: 42, y: 468, font: headingBold, size: 24, fill: c.ink });
  text(screen, "Bangalore | Product designer | Hindu | Graduate", {
    x: 42,
    y: 502,
    font: bodyRegular,
    size: 14,
    fill: c.slate,
    wrapWidth: 298,
    lineHeight: 22,
  });
  pill(screen, 42, 546, "Parents involved", { fill: c.surfaceWarm, text: c.primaryDeep });
  pill(screen, 190, 546, "Vegetarian", { fill: c.surface, text: c.ink, stroke: c.line });
  pill(screen, 42, 586, "Family values align", { fill: c.mint, text: c.emerald });
  button(screen, 42, 618, 118, 42, "Interest", { fill: c.primary });
  button(screen, 170, 618, 92, 42, "Save", { fill: c.surface, text: c.ink, stroke: c.line, radius: 14 });
  button(screen, 272, 618, 92, 42, "More", { fill: c.surface, text: c.ink, stroke: c.line, radius: 14 });
  card(screen, 24, 684, 364, 110, c.surfaceWarm);
  text(screen, "Why this card is stronger", { x: 42, y: 708, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "The user can send interest, favourite, or open more actions without entering the full profile every time.", {
    x: 42,
    y: 738,
    font: bodyRegular,
    size: 14,
    fill: c.slate,
    wrapWidth: 300,
    lineHeight: 22,
  });
  bottomNav(screen, 0);
}

function screenSearch(screen) {
  appBar(screen, "Smart Search", "Fast filters should feel like a superpower");
  searchBar(screen, 126, "Location, profession, community");
  card(screen, 24, 188, 364, 328, c.surface);
  text(screen, "Saved filter bundles", { x: 42, y: 212, font: bodyBold, size: 14, fill: c.ink });
  pill(screen, 42, 242, "Bangalore | 27-33 | Verified", { fill: c.surfaceSoft, text: c.primaryDeep });
  pill(screen, 42, 280, "Parents involved | Vegetarian", { fill: c.mint, text: c.emerald });
  text(screen, "Core filters", { x: 42, y: 334, font: bodyBold, size: 14, fill: c.ink });
  field(screen, 42, 362, 128, "Age min", "26");
  field(screen, 186, 362, 128, "Age max", "33");
  field(screen, 42, 440, 272, "City", "Bangalore, Pune");
  field(screen, 42, 518, 272, "Religion", "Hindu");
  field(screen, 42, 596, 272, "Education", "Graduate and above");
  text(screen, "Toggle filters", { x: 42, y: 674, font: bodyBold, size: 14, fill: c.ink });
  pill(screen, 42, 704, "Verified only", { fill: c.mint, text: c.emerald });
  pill(screen, 156, 704, "Has photo", { fill: c.surface, text: c.ink, stroke: c.line });
  pill(screen, 252, 704, "Recent active", { fill: c.surface, text: c.ink, stroke: c.line });
  button(screen, 24, 778, 174, 56, "Preview 231", { fill: c.surface, text: c.ink, stroke: c.line, radius: 16 });
  button(screen, 214, 778, 174, 56, "Apply filters", { fill: c.primary, shadow: true });
  bottomNav(screen, 1);
}

function screenProfile(screen) {
  rect(screen, 24, 42, 364, 264, c.surfaceSoft, 24, c.line);
  ellipse(screen, 134, 82, 144, 144, c.primary);
  pill(screen, 42, 62, "Verified profile", { fill: c.surface, text: c.emerald, stroke: c.line, dot: true, dotColor: c.emerald });
  text(screen, "Ananya Rao, 28", { x: 42, y: 240, font: headingBold, size: 28, fill: c.ink });
  text(screen, "Mumbai | Architect | Marathi | Serious about marriage", {
    x: 42,
    y: 278,
    font: bodyRegular,
    size: 14,
    fill: c.slate,
    wrapWidth: 290,
    lineHeight: 22,
  });
  pill(screen, 24, 324, "Overview", { fill: c.surfaceSoft, text: c.primaryDeep });
  pill(screen, 112, 324, "Family", { fill: c.surface, text: c.slate, stroke: c.line });
  pill(screen, 194, 324, "Lifestyle", { fill: c.surface, text: c.slate, stroke: c.line });
  pill(screen, 292, 324, "Horoscope", { fill: c.surface, text: c.slate, stroke: c.line });
  card(screen, 24, 372, 364, 124, c.surface);
  text(screen, "Personal snapshot", { x: 42, y: 396, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "Religion", { x: 42, y: 430, font: bodyRegular, size: 12, fill: c.muted });
  text(screen, "Hindu", { x: 42, y: 448, font: bodyMedium, size: 14, fill: c.ink });
  text(screen, "Community", { x: 184, y: 430, font: bodyRegular, size: 12, fill: c.muted });
  text(screen, "Brahmin", { x: 184, y: 448, font: bodyMedium, size: 14, fill: c.ink });
  text(screen, "Mother tongue", { x: 42, y: 470, font: bodyRegular, size: 12, fill: c.muted });
  text(screen, "Marathi", { x: 42, y: 488, font: bodyMedium, size: 14, fill: c.ink });
  text(screen, "Marital status", { x: 184, y: 470, font: bodyRegular, size: 12, fill: c.muted });
  text(screen, "Never married", { x: 184, y: 488, font: bodyMedium, size: 14, fill: c.ink });
  card(screen, 24, 516, 364, 152, c.surface);
  text(screen, "Family, work, and lifestyle", { x: 42, y: 540, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "Nuclear family | Senior architect | Vegetarian | Social drinking | Non smoker", {
    x: 42,
    y: 570,
    font: bodyRegular,
    size: 14,
    fill: c.slate,
    wrapWidth: 300,
    lineHeight: 22,
  });
  text(screen, "About me", { x: 42, y: 628, font: bodyRegular, size: 12, fill: c.muted });
  text(screen, "Thoughtful, grounded, and close to family. Looking for a kind partner with emotional maturity.", {
    x: 42,
    y: 646,
    font: bodyRegular,
    size: 13,
    fill: c.slate,
    wrapWidth: 300,
    lineHeight: 20,
  });
  button(screen, 24, 692, 116, 46, "Interest", { fill: c.primary });
  button(screen, 150, 692, 90, 46, "Save", { fill: c.surface, text: c.ink, stroke: c.line, radius: 14 });
  button(screen, 250, 692, 114, 46, "Share family", { fill: c.surface, text: c.ink, stroke: c.line, radius: 14 });
  button(screen, 24, 778, 364, 56, "Open full detail", { fill: c.primary, shadow: true });
}

function screenActions(screen) {
  rect(screen, 24, 42, 364, 264, c.surfaceSoft, 24, c.line);
  ellipse(screen, 134, 82, 144, 144, c.primary, 0.5);
  text(screen, "Ananya Rao, 28", { x: 42, y: 240, font: headingBold, size: 28, fill: c.ink });
  const veil = rect(screen, 0, 0, 412, 917, c.dark, 0);
  veil.opacity = 0.22;
  const sheet = rect(screen, 0, 458, 412, 459, c.surface, 32, c.line);
  sheet.effects = shadow("#8F6E69", 0.18, -4, 20);
  rect(screen, 176, 474, 60, 6, c.line, 999);
  text(screen, "Profile actions", { x: 24, y: 500, font: headingBold, size: 22, fill: c.ink });
  [
    ["Send interest", c.primaryDeep],
    ["Add to favourites", c.ink],
    ["Share with family", c.ink],
    ["Hide this member", c.ink],
    ["Block this member", c.danger],
    ["Report concern", c.danger],
  ].forEach((item, index) => {
    const y = 548 + index * 56;
    rect(screen, 24, y, 364, 48, index === 0 ? c.surfaceSoft : c.surface, 16, c.line);
    text(screen, item[0], { x: 42, y: y + 16, font: bodyMedium, size: 15, fill: item[1] });
  });
  text(screen, "Hide removes the profile from your feed. Block prevents further contact both ways.", {
    x: 24,
    y: 866,
    font: bodyRegular,
    size: 12,
    fill: c.slate,
    wrapWidth: 328,
    lineHeight: 18,
  });
}

function screenActivity(screen) {
  appBar(screen, "Activity hub", "Everything important in one place", "A");
  pill(screen, 24, 126, "Received 12", { fill: c.surfaceSoft, text: c.primaryDeep });
  pill(screen, 130, 126, "Sent 8", { fill: c.surface, text: c.slate, stroke: c.line });
  pill(screen, 214, 126, "Saved 16", { fill: c.surface, text: c.slate, stroke: c.line });
  pill(screen, 302, 126, "Visitors 5", { fill: c.surface, text: c.slate, stroke: c.line });
  card(screen, 24, 178, 364, 96, c.surface);
  text(screen, "Neha Sharma", { x: 42, y: 202, font: bodyBold, size: 16, fill: c.ink });
  text(screen, "Received interest | Verified | Pune", { x: 42, y: 228, font: bodyRegular, size: 13, fill: c.slate });
  button(screen, 252, 196, 56, 40, "Yes", { fill: c.primary, radius: 14 });
  button(screen, 318, 196, 52, 40, "No", { fill: c.surface, text: c.ink, stroke: c.line, radius: 14 });
  card(screen, 24, 290, 364, 96, c.surface);
  text(screen, "Rahul Mehta", { x: 42, y: 314, font: bodyBold, size: 16, fill: c.ink });
  text(screen, "Shortlisted | Bangalore | Viewed today", { x: 42, y: 340, font: bodyRegular, size: 13, fill: c.slate });
  button(screen, 274, 308, 96, 40, "Open", { fill: c.surface, text: c.ink, stroke: c.line, radius: 14 });
  card(screen, 24, 402, 364, 160, c.surfaceWarm);
  text(screen, "Make this better than competitors", { x: 42, y: 426, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "Do not bury accepted, declined, saved, visitors, hidden, and blocked under separate confusing menus. This screen should be the command center.", {
    x: 42,
    y: 456,
    font: bodyRegular,
    size: 14,
    fill: c.slate,
    wrapWidth: 300,
    lineHeight: 22,
  });
  pill(screen, 42, 528, "Hidden members", { fill: c.surface, text: c.ink, stroke: c.line });
  pill(screen, 170, 528, "Blocked members", { fill: c.surface, text: c.ink, stroke: c.line });
  bottomNav(screen, 2);
}

function screenMessages(screen) {
  appBar(screen, "Messages", "Chat only opens after mutual interest");
  card(screen, 24, 126, 364, 88, c.surface);
  avatar(screen, 42, 146, 48, c.primary, "R");
  text(screen, "Rhea Singh", { x: 104, y: 148, font: bodyBold, size: 16, fill: c.ink });
  text(screen, "That sounds lovely. What kind of books stay with you?", {
    x: 104,
    y: 174,
    font: bodyRegular,
    size: 13,
    fill: c.slate,
    wrapWidth: 208,
    lineHeight: 20,
  });
  pill(screen, 320, 158, "2", { fill: c.surfaceSoft, text: c.primaryDeep });
  card(screen, 24, 230, 364, 88, c.surface);
  avatar(screen, 42, 250, 48, c.peach, "A");
  text(screen, "Amit Verma", { x: 104, y: 252, font: bodyBold, size: 16, fill: c.ink });
  text(screen, "Mutual interest unlocked today", { x: 104, y: 278, font: bodyRegular, size: 13, fill: c.slate });
  card(screen, 24, 346, 364, 126, c.surfaceWarm);
  text(screen, "Suggested improvements", { x: 42, y: 370, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "Add voice and video call entry points later, but keep text chat uncluttered and trustworthy first.", {
    x: 42,
    y: 400,
    font: bodyRegular,
    size: 14,
    fill: c.slate,
    wrapWidth: 300,
    lineHeight: 22,
  });
  pill(screen, 42, 440, "Voice call", { fill: c.surface, text: c.ink, stroke: c.line });
  pill(screen, 148, 440, "Video call", { fill: c.surface, text: c.ink, stroke: c.line });
  bottomNav(screen, 3);
}

function screenChat(screen) {
  avatar(screen, 24, 44, 42, c.primary, "R");
  text(screen, "Rhea Singh", { x: 78, y: 52, font: headingBold, size: 22, fill: c.ink });
  text(screen, "Mutual interest chat", { x: 78, y: 82, font: bodyRegular, size: 13, fill: c.slate });
  pill(screen, 298, 52, "Verified", { fill: c.mint, text: c.emerald });
  card(screen, 24, 126, 364, 66, c.surfaceWarm);
  text(screen, "Stay safe", { x: 42, y: 144, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "Keep sharing inside the app until you are comfortable.", { x: 42, y: 166, font: bodyRegular, size: 13, fill: c.slate });
  rect(screen, 24, 224, 228, 86, c.surface, 20, c.line);
  text(screen, "Hi Aarya, I liked how real your family section felt.", {
    x: 40,
    y: 242,
    font: bodyRegular,
    size: 14,
    fill: c.ink,
    wrapWidth: 196,
    lineHeight: 21,
  });
  text(screen, "7:21 PM", { x: 40, y: 286, font: bodyRegular, size: 11, fill: c.muted });
  rect(screen, 152, 330, 236, 104, c.primary, 20);
  text(screen, "Thank you. I was trying to make it feel honest, not rehearsed.", {
    x: 168,
    y: 348,
    font: bodyRegular,
    size: 14,
    fill: "#FFFFFF",
    wrapWidth: 204,
    lineHeight: 21,
  });
  text(screen, "Seen 7:23 PM", { x: 168, y: 398, font: bodyRegular, size: 11, fill: "#FFEAE6" });
  card(screen, 24, 470, 364, 116, c.surface);
  text(screen, "Conversation assists", { x: 42, y: 494, font: bodyBold, size: 14, fill: c.ink });
  pill(screen, 42, 526, "Ask what honesty looks like to her", { fill: c.surfaceSoft, text: c.primaryDeep });
  pill(screen, 42, 564, "Talk family traditions", { fill: c.mint, text: c.emerald });
  rect(screen, 24, 748, 304, 56, "#FFFDFC", 18, c.line);
  text(screen, "Write a thoughtful message", { x: 44, y: 768, font: bodyRegular, size: 14, fill: c.muted });
  button(screen, 340, 748, 48, 56, "Go", { fill: c.primary, radius: 18 });
}

function screenMyProfile(screen) {
  appBar(screen, "My profile", "Strength, media, preferences, and edits", "A");
  card(screen, 24, 126, 364, 126, c.surface);
  text(screen, "Aarya Sharma", { x: 42, y: 150, font: headingBold, size: 24, fill: c.ink });
  text(screen, "Product manager | Bangalore", { x: 42, y: 184, font: bodyRegular, size: 14, fill: c.slate });
  text(screen, "Profile strength", { x: 42, y: 214, font: bodyBold, size: 13, fill: c.ink });
  rect(screen, 42, 236, 286, 10, c.surfaceSoft, 999);
  rect(screen, 42, 236, 222, 10, c.primary, 999);
  pill(screen, 290, 204, "78%", { fill: c.surfaceSoft, text: c.primaryDeep });
  card(screen, 24, 270, 364, 110, c.surface);
  text(screen, "Photos and trust", { x: 42, y: 294, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "4 photos | OTP verified | Photo privacy on", { x: 42, y: 322, font: bodyRegular, size: 13, fill: c.slate });
  button(screen, 274, 300, 96, 40, "Manage", { fill: c.surface, text: c.ink, stroke: c.line, radius: 14 });
  card(screen, 24, 396, 364, 136, c.surfaceWarm);
  text(screen, "Partner preferences", { x: 42, y: 420, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "27-33 | Hindu | Any manglik | Bangalore or Pune", {
    x: 42,
    y: 450,
    font: bodyRegular,
    size: 14,
    fill: c.slate,
    wrapWidth: 290,
    lineHeight: 22,
  });
  pill(screen, 42, 496, "Syncs with Smart Search", { fill: c.surface, text: c.primaryDeep, stroke: c.line });
  card(screen, 24, 548, 364, 166, c.surface);
  text(screen, "Edit sections", { x: 42, y: 572, font: bodyBold, size: 14, fill: c.ink });
  ["Basic details", "Career", "Family", "Lifestyle", "Horoscope"].forEach((label, index) => {
    const y = 606 + index * 24;
    text(screen, label, { x: 42, y, font: bodyRegular, size: 13, fill: c.slate });
    text(screen, "Edit", { x: 330, y, font: bodyMedium, size: 13, fill: c.primaryDeep });
  });
  bottomNav(screen, 4);
}

function screenPrivacy(screen) {
  appBar(screen, "Privacy and blocked", "Control who sees and contacts you");
  card(screen, 24, 126, 364, 216, c.surface);
  text(screen, "Toggles", { x: 42, y: 150, font: bodyBold, size: 14, fill: c.ink });
  field(screen, 42, 178, 304, "Photo privacy", "On for shortlist + mutual interest");
  field(screen, 42, 256, 304, "Visible in search", "Enabled");
  field(screen, 42, 334, 304, "Contact filters", "Only matching preferences can reach me");
  card(screen, 24, 370, 364, 150, c.surfaceWarm);
  text(screen, "Hidden members", { x: 42, y: 394, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "4 profiles hidden from your feed", { x: 42, y: 422, font: bodyRegular, size: 13, fill: c.slate });
  pill(screen, 42, 456, "Review hidden list", { fill: c.surface, text: c.primaryDeep, stroke: c.line });
  card(screen, 24, 540, 364, 150, c.surface);
  text(screen, "Blocked members", { x: 42, y: 564, font: bodyBold, size: 14, fill: c.ink });
  text(screen, "Blocked members cannot see, contact, or send interest to you.", {
    x: 42,
    y: 592,
    font: bodyRegular,
    size: 13,
    fill: c.slate,
    wrapWidth: 300,
    lineHeight: 20,
  });
  pill(screen, 42, 638, "Manage blocked list", { fill: c.surfaceSoft, text: c.primaryDeep });
  button(screen, 24, 778, 174, 56, "Save settings", { fill: c.surface, text: c.ink, stroke: c.line, radius: 16 });
  button(screen, 214, 778, 174, 56, "Need support", { fill: c.primary, shadow: true });
}

const pageName = "SoulMatch Matrimony Full Flows";
const oldPage = figma.root.children.find((node) => node.type === "PAGE" && node.name === pageName);
if (oldPage) oldPage.remove();

const page = figma.createPage();
page.name = pageName;
await figma.setCurrentPageAsync(page);

const board = figma.createFrame();
board.name = "SoulMatch Matrimony Board";
board.resize(3000, 3320);
board.fills = solid(c.canvas);
board.strokes = [];
board.clipsContent = false;
page.appendChild(board);

text(board, "SoulMatch", { x: 64, y: 56, font: headingBold, size: 42, fill: c.ink, lineHeight: 48 });
text(board, "Full matrimony app flow for production use", {
  x: 64,
  y: 108,
  font: bodyMedium,
  size: 18,
  fill: c.slate,
  wrapWidth: 290,
  lineHeight: 28,
});
text(board, "This page is the implementation target for the Android app.", {
  x: 64,
  y: 154,
  font: bodyMedium,
  size: 14,
  fill: c.primaryDeep,
  wrapWidth: 286,
  lineHeight: 22,
});

[
  ["01 Setup Hub", 420, 180, screenSetupHub],
  ["02 Basic Details", 912, 180, screenBasic],
  ["03 Physical + Career", 1404, 180, screenCareer],
  ["04 Family + Lifestyle", 1896, 180, screenFamily],
  ["05 Horoscope + Preferences", 2388, 180, screenHoroscope],
  ["06 Photos + Privacy", 420, 1228, screenPhotos],
  ["07 Match Home", 912, 1228, screenHome],
  ["08 Smart Search", 1404, 1228, screenSearch],
  ["09 Profile Detail", 1896, 1228, screenProfile],
  ["10 Profile Actions", 2388, 1228, screenActions],
  ["11 Activity Hub", 420, 2276, screenActivity],
  ["12 Messages", 912, 2276, screenMessages],
  ["13 Chat Thread", 1404, 2276, screenChat],
  ["14 My Profile", 1896, 2276, screenMyProfile],
  ["15 Privacy + Blocked", 2388, 2276, screenPrivacy],
].forEach(([label, x, y, render]) => {
  boardLabel(board, x, y - 40, label);
  const screen = phone(board, x, y, label);
  render(screen);
});

figma.currentPage.selection = [board];
figma.viewport.scrollAndZoomIntoView([board]);
