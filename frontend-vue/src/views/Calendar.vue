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
  
  <form @submit.prevent="createMeeting">
    <label>
      <span>Class Code</span>
      <input type="text" name="classCode" />
      <!-- hoping we can get user info and no request year/quater from user data 
       still need class code in case of multi enrollment-->
    </label>
    <label>
      <span>Meeting Date</span>
      <input type="date" name="meetingDate" />
    </label>
    <label>
      <span>Start Time</span>
      <input type="time" name="startTime" />
    </label>
    <label>
      <span>End Time</span>
      <input type="time" name="endTime" />
    </label>
    <label>
      <span>With Who?</span>
      <input type="text" name="recipient" />
    </label>
    <button type="submit">Request Meeting</button>
  </form>
</template>

<script>
export default {
  data() {
    return {
      events: []
    }
  },

  methods: {

    async loadEvents({ start, end }) {//these dates are in the format of { date: 'YYYY-MM-DD' } 
      const startDate = start.date + "T00:00:00";
      const endDate = end.date + "T23:59:59";

      try {
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
    },

    async createMeeting = async (meetingData) => {
    try {
      const response = await fetch('/api/meetings/create-meeting', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            classCode: meetingData.classCode,
            meetingDate: meetingData.meetingDate,
            startTime: meetingData.startTime,
            endTime: meetingData.endTime,
            recipient: meetingData.recipient
        })
      });

      if (!response.ok) {
        throw new Error('Failed to create meeting');
      }

      const data = await response.json();
      console.log('Meeting created:', data);
    } catch (error) {
      console.error('Error creating meeting:', error);
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