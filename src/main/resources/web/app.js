"use strict";

const statuses = ["To Read", "Reading", "Finished"];

const createForm = document.getElementById("create-book-form");
const inventoryElement = document.getElementById("inventory");
const bookCountElement = document.getElementById("book-count");
const createFeedback = document.getElementById("create-action-feedback");
const inventoryFeedback = document.getElementById("inventory-action-feedback");

function setFeedback(element, message, kind) {
  element.textContent = message || "";
  element.className = "feedback";
  if (kind) {
    element.classList.add(kind);
  }
}

function clearCreateFieldErrors() {
  ["title", "author", "status"].forEach((field) => {
    document.getElementById(field + "-error").textContent = "";
  });
}

function showCreateFieldErrors(fieldErrors) {
  clearCreateFieldErrors();
  if (!fieldErrors || typeof fieldErrors !== "object") {
    return;
  }
  Object.entries(fieldErrors).forEach(([field, message]) => {
    const errorElement = document.getElementById(field + "-error");
    if (errorElement) {
      errorElement.textContent = String(message);
    }
  });
}

async function readError(response) {
  try {
    const body = await response.json();
    if (body && body.error) {
      return body.error;
    }
  } catch (error) {
    // A non-JSON response still gets a safe generic message below.
  }
  return {
    code: "INTERNAL_ERROR",
    message: "The request could not be completed",
    fieldErrors: {}
  };
}

async function loadInventory(options = {}) {
  if (options.preserveFeedback !== true) {
    setFeedback(inventoryFeedback, "", "");
  }
  inventoryElement.replaceChildren();
  const loading = document.createElement("p");
  loading.className = "loading-text";
  loading.textContent = "Loading inventory…";
  inventoryElement.appendChild(loading);

  try {
    const response = await fetch("/api/books", {
      headers: { "Accept": "application/json" }
    });
    if (!response.ok) {
      const error = await readError(response);
      setFeedback(inventoryFeedback, error.message, "error");
      const empty = document.createElement("p");
      empty.className = "empty-text";
      empty.textContent = "Inventory unavailable.";
      inventoryElement.replaceChildren(empty);
      bookCountElement.textContent = "Unavailable";
      return;
    }

    const body = await response.json();
    renderInventory(Array.isArray(body.books) ? body.books : []);
  } catch (error) {
    setFeedback(inventoryFeedback, "Unable to contact the reading shelf.", "error");
    const empty = document.createElement("p");
    empty.className = "empty-text";
    empty.textContent = "Inventory unavailable.";
    inventoryElement.replaceChildren(empty);
    bookCountElement.textContent = "Unavailable";
  }
}

function renderInventory(books) {
  inventoryElement.replaceChildren();
  bookCountElement.textContent = books.length === 1 ? "1 book" : books.length + " books";

  if (books.length === 0) {
    const empty = document.createElement("p");
    empty.className = "empty-text";
    empty.textContent = "No books yet.";
    inventoryElement.appendChild(empty);
    return;
  }

  books.forEach((book) => {
    if (!book || typeof book.title !== "string"
        || typeof book.author !== "string"
        || typeof book.status !== "string") {
      return;
    }

    const card = document.createElement("article");
    card.className = "book-card";

    const details = document.createElement("div");
    details.className = "book-detail";
    const title = document.createElement("strong");
    title.textContent = book.title;
    const author = document.createElement("span");
    author.textContent = "by " + book.author;
    details.append(title, author);

    const statusGroup = document.createElement("div");
    const statusLabel = document.createElement("label");
    statusLabel.className = "status-label";
    statusLabel.textContent = "Status";
    const statusSelect = document.createElement("select");
    statuses.forEach((status) => {
      const option = document.createElement("option");
      option.value = status;
      option.textContent = status;
      statusSelect.appendChild(option);
    });
    statusSelect.value = statuses.includes(book.status) ? book.status : statuses[0];
    statusLabel.appendChild(statusSelect);
    statusGroup.appendChild(statusLabel);

    const statusButton = document.createElement("button");
    statusButton.type = "button";
    statusButton.className = "status-button";
    statusButton.textContent = "Change status";
    statusButton.addEventListener("click", () => {
      changeStatus(book.title, book.author, statusSelect.value);
    });

    card.append(details, statusGroup, statusButton);
    inventoryElement.appendChild(card);
  });
}

async function changeStatus(title, author, status) {
  setFeedback(inventoryFeedback, "", "");
  try {
    const response = await fetch("/api/books/status", {
      method: "PATCH",
      headers: {
        "Accept": "application/json",
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ title, author, status })
    });

    if (!response.ok) {
      const error = await readError(response);
      setFeedback(inventoryFeedback, error.message, "error");
      return;
    }

    const body = await response.json();
    if (body.durabilityWarning) {
      setFeedback(inventoryFeedback,
        "Saved with platform-limited crash durability: this platform cannot sync the parent directory.",
        "success");
    } else {
      setFeedback(inventoryFeedback, "", "");
    }
    const changed = body.book;
    if (changed) {
      if (!body.durabilityWarning) {
        setFeedback(
          inventoryFeedback,
          "Status changed to " + changed.status + " for " + changed.title + ".",
          "success"
        );
      }
    } else if (!body.durabilityWarning) {
      setFeedback(inventoryFeedback, "Status changed successfully.", "success");
    }
    await loadInventory({ preserveFeedback: true });
  } catch (error) {
    setFeedback(inventoryFeedback, "Unable to change the book status.", "error");
  }
}

createForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  clearCreateFieldErrors();
  setFeedback(createFeedback, "", "");

  const formData = new FormData(createForm);
  const payload = {
    title: String(formData.get("title") || ""),
    author: String(formData.get("author") || ""),
    status: String(formData.get("status") || "")
  };

  try {
    const response = await fetch("/api/books", {
      method: "POST",
      headers: {
        "Accept": "application/json",
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      const error = await readError(response);
      showCreateFieldErrors(error.fieldErrors);
      setFeedback(createFeedback, error.message, "error");
      return;
    }

    const body = await response.json();
    const created = body.book;
    if (created) {
      if (body.durabilityWarning) {
        setFeedback(createFeedback,
          "Added " + created.title + " to the shelf. Saved with platform-limited crash durability: this platform cannot sync the parent directory.",
          "success");
      } else {
        setFeedback(createFeedback, "Added " + created.title + " to the shelf.", "success");
      }
    } else {
      setFeedback(createFeedback, "Book added successfully.", "success");
    }
    createForm.reset();
    clearCreateFieldErrors();
    await loadInventory();
  } catch (error) {
    setFeedback(createFeedback, "Unable to add the book.", "error");
  }
});

loadInventory();
