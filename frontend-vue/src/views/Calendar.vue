<template>
  <v-container div class="card">
    <v-calendar
      type="month"
      :events="events"
      event-name="name"
      event-start="start"
      event-end="end"
      event-color="color"
      @change="loadEvents"
    />
  </v-container>
</template>

<script>
export default {
  data() {
    return {
      events: []
    }
  },

  methods: {

    async loadEvents({ start, end }) {
      const startDate = start.date + "T00:00:00";
      const endDate = end.date + "T23:59:59";

      try {
        // Replace URL with your Spring Boot endpoint
        const response = await fetch(
          `/api/meetings?start=${encodeURIComponent(startDate)}&end=${encodeURIComponent(endDate)}`
        );

        if (!response.ok) {
          throw new Error("Failed to fetch meetings");
        }

        const data = await response.json();
        
        this.events = data.map(meeting => ({
          name: meeting.classCode + " (" + meeting.status + ")",
          start: meeting.meetingDate + "T" + meeting.startTime,
          end: meeting.meetingDate + "T" + meeting.endTime,
          color: meeting.status === "Confirmed" ? "green" : "red"
        }));

      } catch (error) {
        console.error("Error loading meetings:", error);
      }
    }

  }
}
</script>
<style scoped>
.card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  padding: 18px;
  color: #e5e7eb;
}
h1 { margin: 0 0 8px; }
p { color: #9ca3af; margin: 0; }
</style>