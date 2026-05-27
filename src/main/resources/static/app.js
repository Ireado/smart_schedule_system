const dayMap = {
  MONDAY: "周一",
  TUESDAY: "周二",
  WEDNESDAY: "周三",
  THURSDAY: "周四",
  FRIDAY: "周五"
};

const state = {
  snapshot: null
};

const statusText = document.getElementById("statusText");
const queryTypeEl = document.getElementById("queryType");
const queryTargetEl = document.getElementById("queryTarget");
const timetableTitleEl = document.getElementById("timetableTitle");

document.getElementById("resetDemoBtn").addEventListener("click", async () => {
  await post("/api/demo/reset");
  statusText.textContent = "示例数据已初始化";
  await loadSnapshot();
  clearTable();
});

document.getElementById("generateBtn").addEventListener("click", async () => {
  const result = await post("/api/schedules/generate");
  statusText.textContent = `${result.message}，共生成 ${result.generatedCount} 条课表记录`;
  await loadSnapshot();
});

document.getElementById("queryBtn").addEventListener("click", async () => {
  const type = queryTypeEl.value;
  const id = queryTargetEl.value;
  if (!id) {
    statusText.textContent = "请先选择查询对象";
    return;
  }

  const url = type === "teacher"
    ? `/api/timetables/teachers/${id}`
    : `/api/timetables/classes/${id}`;
  const data = await get(url);
  timetableTitleEl.textContent = `${type === "teacher" ? "教师" : "班级"}课表 - ${data.ownerName}`;
  renderTable(data.entriesByDay, type);
});

queryTypeEl.addEventListener("change", populateTargets);

async function loadSnapshot() {
  const snapshot = await get("/api/snapshot");
  state.snapshot = snapshot;

  document.getElementById("teacherCount").textContent = snapshot.teachers.length;
  document.getElementById("classCount").textContent = snapshot.classes.length;
  document.getElementById("roomCount").textContent = snapshot.rooms.length;
  document.getElementById("scheduleCount").textContent = snapshot.schedules.length;

  populateTargets();
}

function populateTargets() {
  const type = queryTypeEl.value;
  const options = type === "teacher" ? state.snapshot?.teachers ?? [] : state.snapshot?.classes ?? [];
  queryTargetEl.innerHTML = options
    .map(item => `<option value="${item.id}">${item.name} (${item.code})</option>`)
    .join("");
}

function clearTable() {
  renderTable({
    MONDAY: [],
    TUESDAY: [],
    WEDNESDAY: [],
    THURSDAY: [],
    FRIDAY: []
  }, "class");
  timetableTitleEl.textContent = "课表展示";
}

function renderTable(entriesByDay, type) {
  const tbody = document.querySelector("#timetable tbody");
  const rows = [];
  for (let period = 1; period <= 5; period++) {
    const cols = [`<td><strong>第 ${period} 节</strong></td>`];
    for (const day of Object.keys(dayMap)) {
      const entry = (entriesByDay[day] || []).find(item => item.periodIndex === period);
      if (!entry) {
        cols.push('<td class="empty">-</td>');
        continue;
      }

      const secondary = type === "teacher"
        ? `${entry.className} · ${entry.roomName}`
        : `${entry.teacherName} · ${entry.roomName}`;
      cols.push(`
        <td>
          <div class="course-card">
            <strong>${entry.courseName}</strong>
            <small>${secondary}</small>
          </div>
        </td>
      `);
    }
    rows.push(`<tr>${cols.join("")}</tr>`);
  }
  tbody.innerHTML = rows.join("");
}

async function get(url) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(await response.text());
  }
  return response.json();
}

async function post(url) {
  const response = await fetch(url, { method: "POST" });
  if (!response.ok) {
    throw new Error(await response.text());
  }

  const text = await response.text();
  return text ? JSON.parse(text) : {};
}

loadSnapshot().catch(error => {
  statusText.textContent = `初始化失败: ${error.message}`;
  clearTable();
});
