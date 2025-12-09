document.addEventListener("DOMContentLoaded", () => {
  const checkboxes = document.querySelectorAll(".todo-checkbox");

  checkboxes.forEach(checkbox => {
    checkbox.addEventListener("change", () => {
      const todoItem = checkbox.closest(".todo-item");
      todoItem.classList.toggle("completed", checkbox.checked);
      updateProgress();
    });
  });

  function updateProgress() {
    const total = document.querySelectorAll(".todo-item").length;
    const completed = document.querySelectorAll(".todo-item.completed").length;
    const percentage = total === 0 ? 0 : ((completed / total) * 100).toFixed(1);
  }
});