import { computed, inject } from "vue";

export function useMe() {
  const me = inject("me", null);

  const role = computed(() =>
    (me?.value?.role || "STUDENT").toString().trim().toUpperCase()
  );

  const isProfessor = computed(() => role.value === "PROFESSOR");
  const isTA = computed(() => role.value === "TA");

  return { me, role, isProfessor, isTA };
}
