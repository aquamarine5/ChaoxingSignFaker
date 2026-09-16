import { mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";

const projectUrl = "https://zpkavhhjdtghljleztpb.supabase.co";
const apiKey = "sb_publishable_dFuI4bOoYPlDozMXOGKgPg_cCQ0o22B";

const response = await fetch(
  `${projectUrl}/rest/v1/daily_analyser_snapshot?select=snapshot_date,user_count,total_record_sign_count,diff_user_count,diff_total_record_sign_count&order=snapshot_date.asc`,
  { headers: { apikey: apiKey } },
);

if (!response.ok) {
  throw new Error(`Supabase request failed: ${response.status} ${await response.text()}`);
}

const snapshots = await response.json();
if (snapshots.length < 2) {
  throw new Error("At least two daily_analyser_snapshot rows are required.");
}

const outputDirectory = join(process.cwd(), "analysis");
await mkdir(outputDirectory, { recursive: true });

const width = 1200;
const height = 480;
const chart = { left: 88, right: 1112, top: 96, bottom: 392 };
const colors = {
  total: "#0F766E",
  diff: "#EA580C",
  grid: "#D7DEE5",
  axis: "#64748B",
  text: "#1E293B",
  muted: "#64748B",
  background: "#FFFFFF",
};

function escapeXml(value) {
  return String(value).replace(/[<>&'\"]/g, (character) => ({
    "<": "&lt;",
    ">": "&gt;",
    "&": "&amp;",
    "'": "&apos;",
    "\"": "&quot;",
  })[character]);
}

function formatNumber(value) {
  return new Intl.NumberFormat("en-US").format(Math.round(value));
}

function totalAxisBounds(values) {
  const minimum = Math.min(...values);
  const maximum = Math.max(...values);
  if (minimum === maximum) {
    const padding = Math.max(Math.abs(minimum) * 0.2, 1);
    return { minimum: minimum - padding, maximum: maximum + padding * 0.25 };
  }
  return { minimum: minimum * 0.8, maximum: maximum * 1.05 };
}

function diffAxisBounds(values) {
  const maximum = Math.max(...values);
  return { minimum: 0, maximum: Math.max(maximum * 1.4, 1) };
}

function dateLabel(date) {
  return date.slice(5).replace("-", "/");
}

function coordinate(index, count, value, minimum, maximum) {
  return {
    x: chart.left + (index / (count - 1)) * (chart.right - chart.left),
    y: chart.bottom - ((value - minimum) / (maximum - minimum)) * (chart.bottom - chart.top),
  };
}

function smoothPath(points) {
  if (points.length === 0) return "";
  let path = `M ${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`;
  for (let index = 1; index < points.length; index += 1) {
    const previous = points[index - 1];
    const current = points[index];
    const midpointX = (previous.x + current.x) / 2;
    path += ` C ${midpointX.toFixed(2)} ${previous.y.toFixed(2)}, ${midpointX.toFixed(2)} ${current.y.toFixed(2)}, ${current.x.toFixed(2)} ${current.y.toFixed(2)}`;
  }
  return path;
}

function yAxis(minimum, maximum, side) {
  const items = [];
  for (let index = 0; index <= 4; index += 1) {
    const value = minimum + ((maximum - minimum) / 4) * index;
    const y = chart.bottom - ((chart.bottom - chart.top) / 4) * index;
    if (side === "left") {
      items.push(`<text x="${chart.left - 14}" y="${y + 4}" text-anchor="end" class="axis-label">${formatNumber(value)}</text>`);
    } else {
      items.push(`<text x="${chart.right + 14}" y="${y + 4}" text-anchor="start" class="axis-label">${formatNumber(value)}</text>`);
    }
  }
  return items.join("\n");
}

function gridLines() {
  return Array.from({ length: 5 }, (_, index) => {
    const y = chart.bottom - ((chart.bottom - chart.top) / 4) * index;
    return `<line x1="${chart.left}" y1="${y}" x2="${chart.right}" y2="${y}" class="grid"/>`;
  }).join("\n");
}

function xAxis(points) {
  const positions = [0, 0.2, 0.4, 0.6, 0.8, 1];
  return positions.map((position) => {
    const index = Math.round(position * (points.length - 1));
    const point = points[index];
    return `<line x1="${point.x}" y1="${chart.bottom}" x2="${point.x}" y2="${chart.bottom + 5}" class="axis"/>
      <text x="${point.x}" y="${chart.bottom + 27}" text-anchor="middle" class="axis-label">${dateLabel(snapshots[index].snapshot_date)}</text>`;
  }).join("\n");
}

function createChart({ title, totalKey, diffKey, totalLegend, diffLegend }) {
  const totalBounds = totalAxisBounds(snapshots.map((row) => row[totalKey]));
  const diffRows = snapshots.filter((row) => row[diffKey] !== null);
  const diffBounds = diffAxisBounds(diffRows.map((row) => row[diffKey]));
  const totalPoints = snapshots.map((row, index) => coordinate(index, snapshots.length, row[totalKey], totalBounds.minimum, totalBounds.maximum));
  const diffPoints = snapshots.map((row) => row[diffKey])
    .map((value, index) => value === null ? null : coordinate(index, snapshots.length, value, diffBounds.minimum, diffBounds.maximum));
  const visibleDiffPoints = diffPoints.filter(Boolean);
  const maximumDiff = Math.max(...diffRows.map((row) => row[diffKey]));
  const maximumDiffIndex = snapshots.findIndex((row) => row[diffKey] === maximumDiff);
  const maximumDiffPoint = diffPoints[maximumDiffIndex];
  const maximumDiffLabel = formatNumber(maximumDiff);
  const maximumDiffLabelWidth = maximumDiffLabel.length * 8 + 12;
  const latest = snapshots.at(-1);
  const latestTotal = latest[totalKey];
  const latestDiff = latest[diffKey] ?? 0;

  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" role="img" aria-labelledby="title description">
  <title id="title">${escapeXml(title)}</title>
  <desc id="description">Daily values from ${snapshots[0].snapshot_date} through ${latest.snapshot_date}. The teal line uses the left axis and the orange line uses the right axis.</desc>
  <style>
    text { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
    .title { fill: ${colors.text}; font-size: 23px; font-weight: 700; }
    .subtitle { fill: ${colors.muted}; font-size: 13px; }
    .axis-label { fill: ${colors.axis}; font-size: 12px; }
    .grid { stroke: ${colors.grid}; stroke-width: 1; }
    .axis { stroke: ${colors.axis}; stroke-width: 1; }
    .legend { fill: ${colors.text}; font-size: 13px; }
    .value { font-size: 13px; font-weight: 700; }
  </style>
  <rect width="${width}" height="${height}" fill="${colors.background}"/>
  <text x="${chart.left}" y="38" class="title">${escapeXml(title)}</text>
  <text x="${chart.left}" y="61" class="subtitle">${snapshots[0].snapshot_date} to ${latest.snapshot_date}</text>
  <line x1="${chart.right - 315}" y1="33" x2="${chart.right - 291}" y2="33" stroke="${colors.total}" stroke-width="3" stroke-linecap="round"/>
  <text x="${chart.right - 282}" y="37" class="legend">${escapeXml(totalLegend)} (left)</text>
  <line x1="${chart.right - 135}" y1="33" x2="${chart.right - 111}" y2="33" stroke="${colors.diff}" stroke-width="3" stroke-linecap="round"/>
  <text x="${chart.right - 102}" y="37" class="legend">${escapeXml(diffLegend)} (right)</text>
  ${gridLines()}
  <line x1="${chart.left}" y1="${chart.top}" x2="${chart.left}" y2="${chart.bottom}" class="axis"/>
  <line x1="${chart.right}" y1="${chart.top}" x2="${chart.right}" y2="${chart.bottom}" class="axis"/>
  <line x1="${chart.left}" y1="${chart.bottom}" x2="${chart.right}" y2="${chart.bottom}" class="axis"/>
  ${yAxis(totalBounds.minimum, totalBounds.maximum, "left")}
  ${yAxis(diffBounds.minimum, diffBounds.maximum, "right")}
  ${xAxis(totalPoints)}
  <path d="${smoothPath(totalPoints)}" fill="none" stroke="${colors.total}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="${smoothPath(visibleDiffPoints)}" fill="none" stroke="${colors.diff}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
  <circle cx="${maximumDiffPoint.x}" cy="${maximumDiffPoint.y}" r="5" fill="${colors.diff}" stroke="${colors.background}" stroke-width="2"/>
  <rect x="${maximumDiffPoint.x - maximumDiffLabelWidth / 2}" y="${maximumDiffPoint.y - 33}" width="${maximumDiffLabelWidth}" height="22" rx="3" fill="${colors.background}" stroke="${colors.diff}" stroke-width="1"/>
  <text x="${maximumDiffPoint.x}" y="${maximumDiffPoint.y - 17}" text-anchor="middle" class="value" fill="${colors.diff}">${maximumDiffLabel}</text>
  <circle cx="${totalPoints.at(-1).x}" cy="${totalPoints.at(-1).y}" r="4.5" fill="${colors.total}" stroke="${colors.background}" stroke-width="2"/>
  <circle cx="${visibleDiffPoints.at(-1).x}" cy="${visibleDiffPoints.at(-1).y}" r="4.5" fill="${colors.diff}" stroke="${colors.background}" stroke-width="2"/>
  <text x="${chart.left}" y="${height - 18}" class="subtitle">Latest snapshot</text>
  <text x="${chart.left + 100}" y="${height - 18}" class="value" fill="${colors.total}">${formatNumber(latestTotal)}</text>
  <text x="${chart.left + 190}" y="${height - 18}" class="subtitle">Daily change</text>
  <text x="${chart.left + 285}" y="${height - 18}" class="value" fill="${colors.diff}">+${formatNumber(latestDiff)}</text>
  <text x="${chart.left + 380}" y="${height - 18}" class="subtitle">Update date: ${latest.snapshot_date}</text>
</svg>`;
}

await writeFile(
  join(outputDirectory, "signCount.svg"),
  createChart({
    title: "Recorded Sign Counts",
    totalKey: "total_record_sign_count",
    diffKey: "diff_total_record_sign_count",
    totalLegend: "Total records",
    diffLegend: "Daily change",
  }),
);

await writeFile(
  join(outputDirectory, "userCount.svg"),
  createChart({
    title: "Analyser User Counts",
    totalKey: "user_count",
    diffKey: "diff_user_count",
    totalLegend: "Users",
    diffLegend: "Daily change",
  }),
);

console.log(`Generated analysis charts from ${snapshots.length} snapshots.`);
