<script setup>
</script>

<template>
  <div class="grid">
    <!-- Left: Syllabus Overview -->
    <section class="card big">
      <div class="card-header">
        <div>
          <h2>Syllabus Overview</h2>
          <p class="muted">
            Content organized from professor-uploaded syllabus PDF.
          </p>
        </div>
      </div>

      <div class="content">
        <div class="placeholder">
          <div class="line w60" />
          <div class="line w85" />
          <div class="line w70" />
          <div class="line w90" />
          <div class="line w55" />
        </div>

        <div class="mini-grid">
          <div class="mini">
            <div class="mini-title">Course</div>
            <div class="mini-value">—</div>
          </div>
          <div class="mini">
            <div class="mini-title">Professor</div>
            <div class="mini-value">—</div>
          </div>
          <div class="mini">
            <div class="mini-title">Key Policies</div>
            <div class="mini-value">—</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Right: Upcoming This Week -->
    <section class="card side">
      <div class="card-header">
        <div>
          <h2>Upcoming This Week</h2>
        </div>
        <button class="btn ghost">Import Canvas .ics</button>
      </div>

      <div class="list">
        <div class="item">
          <div class="badge">Mon</div>
          <div class="info">
            <div class="title">—</div>
            <div class="sub">No assignments loaded yet</div>
          </div>
        </div>
        <div class="item">
          <div class="badge">Wed</div>
          <div class="info">
            <div class="title">—</div>
            <div class="sub">Connect Canvas to populate</div>
          </div>
        </div>
        <div class="item">
          <div class="badge">Fri</div>
          <div class="info">
            <div class="title">—</div>
            <div class="sub">Deadlines will show here</div>
          </div>
        </div>
      </div>

      <div class="footer-note">
        Once backend is ready, this becomes a real list with due dates and course tags.
      </div>
    </section>

    <!-- Bottom row: Join Class -->
    <section class="card small">
      <h3>Join a Class</h3>
      <p class="muted">
        Enter a class code provided by your professor.
      </p>
      <div class="row">
        <input class="input" placeholder="Class code (ex: ABC123)" />
        <button class="btn">Join</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 18px;
}

.card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  padding: 16px;
  color: #e5e7eb;
  box-shadow: 0 18px 40px rgba(0,0,0,0.25);
}

.big {
  min-height: 360px;
}

.side {
  min-height: 360px;
}

.small {
  grid-column: span 1;
}

.card-header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

h2 {
  margin: 0;
  font-size: 18px;
}
h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.muted {
  margin: 6px 0 0;
  color: #9ca3af;
  font-size: 13px;
}

.btn {
  border: none;
  background: #2563eb;
  color: white;
  font-weight: 800;
  padding: 10px 12px;
  border-radius: 14px;
  cursor: pointer;
}

.btn:hover {
  background: #1d4ed8;
}

.btn.ghost {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.10);
}

.btn.ghost:hover {
  background: rgba(255,255,255,0.09);
}

.content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.placeholder {
  padding: 14px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px dashed rgba(255,255,255,0.10);
}

.line {
  height: 10px;
  border-radius: 999px;
  background: rgba(255,255,255,0.08);
  margin: 10px 0;
}

.w55 { width: 55%; }
.w60 { width: 60%; }
.w70 { width: 70%; }
.w85 { width: 85%; }
.w90 { width: 90%; }

.mini-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.mini {
  padding: 12px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
}

.mini-title {
  font-size: 12px;
  color: #9ca3af;
}

.mini-value {
  margin-top: 6px;
  font-weight: 900;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
}

.badge {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: rgba(37,99,235,0.25);
  border: 1px solid rgba(37,99,235,0.35);
  color: #bfdbfe;
  font-weight: 900;
}

.title {
  font-weight: 900;
}

.sub {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.footer-note {
  margin-top: 12px;
  font-size: 12px;
  color: #9ca3af;
}

.row {
  margin-top: 12px;
  display: flex;
  gap: 10px;
}

.input {
  flex: 1;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(255,255,255,0.10);
  background: rgba(15,23,42,0.6);
  color: #e5e7eb;
  outline: none;
}

.input:focus {
  box-shadow: 0 0 0 2px rgba(37,99,235,0.5);
  border-color: rgba(37,99,235,0.6);
}

@media (max-width: 980px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .mini-grid {
    grid-template-columns: 1fr;
  }
}
</style>