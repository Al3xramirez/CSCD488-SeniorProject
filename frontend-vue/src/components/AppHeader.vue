<script setup>
</script>

<!-- AppHeader.vue: Top header bar with title, notifications, and profile -->
 
<template>
  <header class="header">
    <div class="left">
      <div class="title">SyllabusSync</div>
    </div>

    <div class="right">
      <button class="icon-btn" title="Notifications (placeholder)">
        🔔
      </button>

      <div class="profile">
        <div class="avatar">👤</div>
        <div class="meta">
          <div class="name">Student</div>
          <div class="sub">Profile</div>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: rgba(255,255,255,0.03);
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.left {
  display: flex;
  align-items: center;
}

.title {
  font-weight: 900;
  color: #e5e7eb;
  letter-spacing: 0.2px;
  font-size: 18px;
}

.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-btn {
  height: 40px;
  width: 40px;
  border-radius: 14px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04);
  color: #e5e7eb;
  cursor: pointer;
}

.icon-btn:hover {
  background: rgba(255,255,255,0.07);
}

.profile {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 16px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04);
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(255,255,255,0.07);
}

.name {
  font-weight: 800;
  font-size: 13px;
  color: #e5e7eb;
}

.sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}
</style>