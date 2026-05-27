const state = {
  accountNo: "",
  me: null,
  snapshot: null,
  filterOptions: null
};

const PERIODS = 8;
const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"];
const DAY_LABELS = {
  MONDAY: "周一",
  TUESDAY: "周二",
  WEDNESDAY: "周三",
  THURSDAY: "周四",
  FRIDAY: "周五"
};

const loginView = document.getElementById("loginView");
const appView = document.getElementById("appView");
const loginMessage = document.getElementById("loginMessage");
const statusText = document.getElementById("statusText");
const timetableTitle = document.getElementById("timetableTitle");
const majorFilter = document.getElementById("majorFilter");
const classFilter = document.getElementById("classFilter");
const queryType = document.getElementById("queryType");
const queryTarget = document.getElementById("queryTarget");
const loadingModal = document.getElementById("loadingModal");
const prevWeekBtn = document.getElementById("prevWeek");
const nextWeekBtn = document.getElementById("nextWeek");
const weekLabel = document.getElementById("weekLabel");

document.getElementById("loginForm").addEventListener("submit", handleLogin);
document.getElementById("logoutBtn").addEventListener("click", logout);
document.getElementById("resetDemoBtn").addEventListener("click", resetDemo);
document.getElementById("generateBtn").addEventListener("click", generateSchedule);
document.getElementById("queryBtn").addEventListener("click", queryAdminTimetable);
majorFilter.addEventListener("change", refreshAdminFilters);
classFilter.addEventListener("change", refreshAdminFilters);
queryType.addEventListener("change", populateQueryTargets);
prevWeekBtn.addEventListener("click", () => navigateWeek(-1));
nextWeekBtn.addEventListener("click", () => navigateWeek(1));

showLoggedOut();

async function handleLogin(event) {
  event.preventDefault();
  loginMessage.textContent = "";
  try {
    const result = await request("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({
        accountNo: document.getElementById("accountNo").value.trim(),
        password: document.getElementById("password").value
      })
    }, false);
    state.accountNo = result.accountNo;
    sessionStorage.setItem("accountNo", state.accountNo);
    await bootApp();
  } catch (error) {
    showLoggedOut();
    loginMessage.textContent = toMessage(error);
  }
}

async function bootApp() {
  await loadMe();
  try { await loadSnapshot(); } catch (e) { statusText.textContent = `数据加载失败: ${toMessage(e)}`; return; }
  await loadConfig();
  showLoggedIn();
  renderDashboard();
  state.currentWeek = 1;
  updateWeekLabel();
  if (state.me.role === "ADMIN") {
    await refreshAdminFilters().catch(e => {
      statusText.textContent = `筛选器加载失败: ${toMessage(e)}`;
    });
    clearTable();
    timetableTitle.textContent = "管理员课表查询";
    if (!statusText.textContent.includes("失败")) {
      statusText.textContent = "可按专业、班级筛选老师和学生的学期课表";
    }
    state.queryMode = "admin";
  } else {
    state.queryMode = "self";
    await loadMyTimetable();
  }
}

function showLoggedOut() {
  state.me = null;
  state.snapshot = null;
  state.filterOptions = null;
  loginView.classList.remove("hidden");
  appView.classList.add("hidden");
  document.getElementById("adminPanel").classList.add("hidden");
  loginMessage.textContent = "";
}

function showLoggedIn() {
  loginView.classList.add("hidden");
  appView.classList.remove("hidden");
  document.getElementById("adminPanel").classList.toggle("hidden", state.me.role !== "ADMIN");
}

function logout() {
  sessionStorage.removeItem("accountNo");
  state.accountNo = "";
  showLoggedOut();
}

async function loadMe() {
  state.me = await request("/api/auth/me");
}

async function loadSnapshot() {
  state.snapshot = await request("/api/snapshot");
}

function renderDashboard() {
  document.getElementById("welcomeText").textContent = `欢迎，${state.me.displayName}`;
  document.getElementById("roleText").textContent = roleLabel(state.me.role, state.me.major, state.me.className);
  document.getElementById("teacherCount").textContent = state.snapshot.teachers.length;
  document.getElementById("studentCount").textContent = state.snapshot.students.length;
  document.getElementById("classCount").textContent = state.snapshot.classes.length;
  document.getElementById("roomCount").textContent = state.snapshot.rooms.length;
  document.getElementById("scheduleCount").textContent = state.snapshot.schedules.length;
}

async function loadMyTimetable() {
  try {
    const data = await request(`/api/timetables/me?week=${state.currentWeek}`);
    timetableTitle.textContent = state.me.role === "STUDENT" ? "我的个人学期课表" : "我的授课学期课表";
    renderTable(data.entriesByDay, state.me.role.toLowerCase());
    updateWeekFromResponse(data);
    statusText.textContent = state.me.role === "STUDENT" ? "已加载你的个人课表" : "已加载你的授课课表";
  } catch (error) {
    statusText.textContent = `加载课表失败: ${toMessage(error)}`;
  }
}

async function resetDemo() {
  try {
    await request("/api/demo/reset", { method: "POST" });
    await loadSnapshot();
    renderDashboard();
    await refreshAdminFilters();
    clearTable();
    statusText.textContent = "随机大学数据已重建";
  } catch (error) {
    statusText.textContent = `数据重建失败: ${toMessage(error)}`;
  }
}

async function generateSchedule() {
  loadingModal.classList.remove("hidden");
  try {
    const result = await request("/api/schedules/generate", { method: "POST" });
    state.currentWeek = 1;
    updateWeekLabel();
    await loadSnapshot();
    renderDashboard();
    statusText.textContent = `${result.message}，生成 ${result.generatedCount} 条个人课表记录`;
  } catch (error) {
    statusText.textContent = `排课失败: ${toMessage(error)}`;
  } finally {
    loadingModal.classList.add("hidden");
  }
}

async function refreshAdminFilters() {
  const params = new URLSearchParams();
  if (majorFilter.value) params.set("major", majorFilter.value);
  if (classFilter.value && classFilter.value !== "ALL") params.set("classId", classFilter.value);

  if (!majorFilter.dataset.loaded) {
    majorFilter.innerHTML = [`<option value="">全部专业</option>`, ...(state.me.majorOptions || []).map(item => `<option value="${item}">${item}</option>`)].join("");
    majorFilter.dataset.loaded = "true";
  }

  state.filterOptions = await request(`/api/admin/filter-options${params.toString() ? `?${params}` : ""}`);
  const classes = [`<option value="ALL">全部班级</option>`, ...state.filterOptions.classes.map(item => optionHtml(item))].join("");
  classFilter.innerHTML = classes;
  populateQueryTargets();
}

function populateQueryTargets() {
  if (!state.filterOptions) return;
  let options = [];
  if (queryType.value === "teacher") {
    options = state.filterOptions.teachers;
  } else if (queryType.value === "student") {
    options = state.filterOptions.students;
  } else {
    options = state.filterOptions.classes;
  }
  queryTarget.innerHTML = options.map(item => optionHtml(item)).join("");
}

async function queryAdminTimetable() {
  const id = queryTarget.value;
  if (!id) {
    statusText.textContent = "请先选择查询对象";
    return;
  }
  let url = `/api/timetables/classes/${id}?week=${state.currentWeek}`;
  let type = "class";
  if (queryType.value === "teacher") {
    url = `/api/timetables/teachers/${id}?week=${state.currentWeek}`;
    type = "teacher";
  } else if (queryType.value === "student") {
    url = `/api/timetables/students/${id}?week=${state.currentWeek}`;
    type = "student";
  }
  const data = await request(url);
  timetableTitle.textContent = `${labelOfType(type)} - ${data.ownerName}`;
  renderTable(data.entriesByDay, type);
  updateWeekFromResponse(data);
  statusText.textContent = "已加载筛选结果";
}

function renderTable(entriesByDay, type) {
  const tbody = document.querySelector("#timetable tbody");
  const rows = [];
  for (let period = 1; period <= PERIODS; period++) {
    const cells = [`<td><strong>第 ${period} 节</strong></td>`];
    for (const day of DAYS) {
      const entries = (entriesByDay[day] || []).filter(item => item.periodIndex === period);
      if (!entries.length) {
        cells.push('<td class="empty">-</td>');
        continue;
      }
      cells.push(`<td>${entries.map(item => cardHtml(item, type)).join("")}</td>`);
    }
    rows.push(`<tr>${cells.join("")}</tr>`);
  }
  tbody.innerHTML = rows.join("");
}

function cardHtml(entry, type) {
  let secondary = `${entry.teacherName} · ${entry.roomName}`;
  if (type === "teacher") {
    secondary = `${entry.className} · ${entry.roomName}`;
  } else if (type === "student") {
    secondary = `${entry.teacherName} · ${entry.roomName}`;
  } else if (type === "class") {
    secondary = `${entry.teacherName} · ${entry.roomName}`;
  }
  return `<div class="course-card"><strong>${entry.courseName}</strong><small>${secondary}</small></div>`;
}

function clearTable() {
  const empty = Object.fromEntries(DAYS.map(day => [day, []]));
  renderTable(empty, "class");
}

function roleLabel(role, major, className) {
  if (role === "STUDENT") return `学生账号 · ${major} · ${className} · 仅可查看个人课表`;
  if (role === "TEACHER") return "教师账号 · 仅可查看本人授课课表";
  return "管理员账号 · 可生成数据、执行排课、按专业/班级筛选老师和学生课表";
}

function labelOfType(type) {
  if (type === "student") return "学生个人课表";
  if (type === "teacher") return "教师课表";
  return "班级课表";
}

function optionHtml(item) {
  return `<option value="${item.id}">${item.name} (${item.code})</option>`;
}

async function loadConfig() {
  try {
    const config = await request("/api/config");
    state.totalWeeks = config.totalWeeks || 18;
  } catch (e) {
    state.totalWeeks = 18;
  }
}

function navigateWeek(delta) {
  const next = state.currentWeek + delta;
  if (next < 1 || next > state.totalWeeks) return;
  state.currentWeek = next;
  updateWeekLabel();
  if (state.queryMode === "admin") {
    queryAdminTimetable();
  } else if (state.queryMode === "self") {
    loadMyTimetable();
  }
}

function updateWeekLabel() {
  weekLabel.textContent = `第 ${state.currentWeek} 周`;
  prevWeekBtn.disabled = state.currentWeek <= 1;
  nextWeekBtn.disabled = state.currentWeek >= state.totalWeeks;
}

function updateWeekFromResponse(data) {
  if (data.week) {
    state.currentWeek = data.week;
  }
  if (data.totalWeeks) {
    state.totalWeeks = data.totalWeeks;
  }
  updateWeekLabel();
}

function authHeaders() {
  return state.accountNo ? { "X-Account-No": state.accountNo } : {};
}

async function request(url, options = {}, withAuth = true) {
  const response = await fetch(url, {
    ...options,
    headers: {
      ...(withAuth ? authHeaders() : {}),
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.headers || {})
    }
  });
  if (!response.ok) {
    const raw = await response.text();
    let message = raw;
    try {
      const parsed = JSON.parse(raw);
      message = parsed.message || parsed.error || raw;
    } catch (e) {
    }
    throw new Error(message);
  }
  const text = await response.text();
  return text ? JSON.parse(text) : {};
}

function toMessage(error) {
  return String(error.message || error).replace(/^Error:\s*/, "").trim();
}

const restoredAccount = sessionStorage.getItem("accountNo");
if (restoredAccount) {
  state.accountNo = restoredAccount;
  bootApp().catch(() => {
    logout();
  });
}
