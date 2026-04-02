import { ref, computed, onMounted, onUnmounted } from "vue";

/**
 * Provides a reactive `now` that ticks every 30 seconds,
 * plus helpers to compute live countdown labels for meeting events.
 *
 * @param {import('vue').Ref<Array>} events - reactive ref of calendar event objects
 */
export function useMeetingReminders(events) {
  const now = ref(new Date());
  let timer;

  onMounted(() => {
    timer = setInterval(() => {
      now.value = new Date();
    }, 30_000);
  });

  onUnmounted(() => clearInterval(timer));

  /**
   * Returns a human-readable countdown string, or null if not imminent.
   * Shows labels from "Started 10 min ago" up to "in 90 min".
   */
  function reminderLabel(isoString) {
    if (!isoString) return null;
    const start = new Date(isoString);
    if (isNaN(start)) return null;
    const mins = Math.round((start - now.value) / 60_000);
    if (mins < -10) return null; // started >10 min ago — not relevant
    if (mins < 0) return `Started ${Math.abs(mins)} min ago`;
    if (mins === 0) return "Starting now";
    if (mins <= 90) return `in ${mins} min`;
    return null; // too far away
  }

  /**
   * The soonest non-cancelled, non-allDay event within the next 90 minutes
   * (or that started within the last 10 minutes).
   */
  const upcomingReminder = computed(() => {
    if (!events?.value?.length) return null;

    const candidates = events.value
      .filter((e) => !e.isCancelled && !e.allDay && e.startAt)
      .sort((a, b) => new Date(a.startAt) - new Date(b.startAt));

    for (const e of candidates) {
      const mins = Math.round((new Date(e.startAt) - now.value) / 60_000);
      if (mins >= -10 && mins <= 90) {
        return { event: e, label: reminderLabel(e.startAt) };
      }
    }
    return null;
  });

  return { now, reminderLabel, upcomingReminder };
}
