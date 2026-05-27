const state = {
  accountNo: localStorage.getItem("accountNo") || "",
  me: null,
  snapshot: null
};
const PERIODS_PER_DAY = 6;

const dayMap = {
  MONDAY: "周一",
  TUESDAY: "周二",
  WEDNESDAY: "周三",
  THURSDAY: "周四",
  FRIDAY: "周五"
};

const loginPanel = document.getElementById("loginPanel");
const appPanel = document.getElementById("appPanel");
const loginMessage = document.getElementById("loginMessage");
const statusText = document.getElementById("statusText");
const queryTypeEl = document.getElementById("queryType");
const queryTargetEl = document.getElementById("queryTarget");
const timetableTitleEl = document.getElementById("timetableTitle");

document.getElementById("loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const result = await request("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({
        accountNo: document.getElementById("accountNo").value.trim(),
        password: document.getElementById("password").value
      })
    }, false);
    state.accountNo = result.accountNo;
    localStorage.setItem("accountNo", state.accountNo);
    loginMessage.textContent = `${result.displayName} 登录成功`;
    await bootApp();
  } catch (error) {
    loginMessage.textContent = extractMessage(error);
  }
});

document.getElementById("logoutBtn").addEventListener("click", () => {
  localStorage.removeItem("accountNo");
  state.accountNo = "";
  state.me = null;
  loginPanel.classList.remove("hidden");
  appPanel.classList.add("hidden");
});

document.getElementById("resetDemoBtn").addEventListener("click", async () => {
  await request("/api/demo/reset", { method: "POST" });
  statusText.textContent = "已生成新的大学随机数据";
  await loadAppData();
});

document.getElementById("generateBtn").addEventListener("click", async () => {
  const result = await request("/api/schedules/generate", { method: "POST" });
  statusText.textContent = `${result.message}，共生成 ${result.generatedCount} 条记录`;
  await loadAppData();
});

document.getElementById("queryBtn").addEventListener("click", async () => {
  const type = queryTypeEl.value;
  const id = queryTargetEl.value;
  if (!id) {
    statusText.textContent = "请先选择查询对象";
    return;
  }
  const url = type === "teacher" ? `/api/timetables/teachers/${id}` : `/api/timetables/classes/${id}`;
  const data = await request(url);
  timetableTitleEl.textContent = `${type === "teacher" ? "教师" : "班级"}课表 - ${data.ownerName}`;
  renderTable(data.entriesByDay, type);
});

queryTypeEl.addEventListener("change", populateAdminTargets);

async function bootApp() {
  await loadAppData();
  loginPanel.classList.add("hidden");
  appPanel.classList.remove("hidden");
}

async function loadAppData() {
  state.me = await request("/api/auth/me");
  state.snapshot = await request("/api/snapshot");

  document.getElementById("welcomeText").textContent = `欢迎，${state.me.displayName}`;
  document.getElementById("roleText").textContent = roleLabel(state.me.role);
  document.getElementById("teacherCount").textContent = state.snapshot.teachers.length;
  document.getElementById("studentCount").textContent = state.snapshot.students.length;
  document.getElementById("classCount").textContent = state.snapshot.classes.length;
  document.getElementById("roomCount").textContent = state.snapshot.rooms.length;
  document.getElementById("scheduleCount").textContent = state.snapshot.schedules.length;

  const isAdmin = state.me.role === "ADMIN";
  document.getElementById("adminControls").classList.toggle("hidden", !isAdmin);
  populateAdminTargets();

  if (isAdmin) {
    timetableTitleEl.textContent = "管理员课表查看";
    clearTable();
    statusText.textContent = "管理员可以重新生成数据并执行排课";
    return;
  }

  const myTimetable = await request("/api/timetables/me");
  timetableTitleEl.textContent = state.me.role === "STUDENT" ? "我的课表" : "我的授课课表";
  renderTable(myTimetable.entriesByDay, state.me.role === "TEACHER" ? "teacher" : "student");
  statusText.textContent = "当前账号只有课表查看权限";
}

function populateAdminTargets() {
  if (!state.me || state.me.role !== "ADMIN") {
    return;
  }
  const source = queryTypeEl.value === "teacher" ? state.me.teacherOptions : state.me.classOptions;
  queryTargetEl.innerHTML = source.map(item => `<option value="${item.id}">${item.name} (${item.code})</option>`).join("");
}

function renderTable(entriesByDay, type) {
  const tbody = document.querySelector("#timetable tbody");
  const rows = [];
  for (let period = 1; period <= PERIODS_PER_DAY; period++) {
    const cells = [`<td><strong>第 ${period} 节</strong></td>`];
    for (const day of Object.keys(dayMap)) {
      const entry = (entriesByDay[day] || []).find(item => item.periodIndex === period);
      if (!entry) {
        cells.push('<td class="empty">-</td>');
        continue;
      }
      let secondary = `${entry.teacherName} · ${entry.roomName}`;
      if (type === "teacher") {
        secondary = `${entry.className} · ${entry.roomName}`;
      } else if (type === "student") {
        secondary = `${entry.teacherName} · ${entry.roomName} · ${entry.className}`;
      }
      cells.push(`
        <td>
          <div class="course-card">
            <strong>${entry.courseName}</strong>
            <small>${secondary}</small>
          </div>
        </td>
      `);
    }
    rows.push(`<tr>${cells.join("")}</tr>`);
  }
  tbody.innerHTML = rows.join("");
}

function clearTable() {
  renderTable({
    MONDAY: [],
    TUESDAY: [],
    WEDNESDAY: [],
    THURSDAY: [],
    FRIDAY: []
  }, "class");
}

function roleLabel(role) {
  if (role === "STUDENT") {
    return "大学生账号：仅可查看本人班级课表";
  }
  if (role === "TEACHER") {
    return "大学教师账号：仅可查看本人授课课表";
  }
  return "管理员账号：可生成测试数据、执行排课、查看任意教师/班级课表";
}

function authHeaders() {
  return state.accountNo ? { "X-Account-No": state.accountNo } : {};
}

async function request(url, options = {}, withAuth = true) {
  const response = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(withAuth ? authHeaders() : {}),
      ...(options.headers || {})
    }
  });
  if (!response.ok) {
    throw new Error(await response.text());
  }
  const text = await response.text();
  return text ? JSON.parse(text) : {};
}

function extractMessage(error) {
  return String(error.message || error)
    .replace(/^Error:\s*/, "")
    .trim();
}

if (state.accountNo) {
  bootApp().catch(() => {
    localStorage.removeItem("accountNo");
    state.accountNo = "";
  });
}
