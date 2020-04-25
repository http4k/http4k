// spinner loading
$(window).on("load", function() {
  // Spinner Loading
  $(".spinner-loading").fadeOut(function() {
    $(this).remove();
  });
});

// jquery ready start
$(document).ready(function() {
  // back to prev page
  $("a.back").click(function() {
    parent.history.back();
    return false;
  });

  // toggle sidebar
  $(".sideToggle").on("click", function() {
    $(".aside").toggleClass("no-side");
  });

  // active side bar links
  $(".intro .nav-link").on("click", function() {
    $(this)
      .parent(".nav-item")
      .addClass("active");
    $(this)
      .parent(".nav-item")
      .siblings()
      .removeClass("active");
  });
  $(".intro .dropdown-item").on("click", function() {
    $(this).addClass("active");
    $(this)
      .siblings()
      .removeClass("active");
  });

  // copy code to clipboard
  $(".getCode").on("click", function() {
    let allCode = $(this)
      .parent(".title")
      .siblings("pre");

    let $temp = $("<input>");
    $("body").append($temp);
    $temp.val($(allCode).text()).select();
    document.execCommand("copy");
    $temp.remove();

    $(this).attr("title", "Copied");
    $(this)
      .tooltip("dispose")
      .tooltip({ title: "Copied" })
      .tooltip("show");
    $(this).on("hidden.bs.tooltip", function() {
      $(this).attr("title", "Copy to clipboard");
      $(this)
        .tooltip("dispose")
        .tooltip({ title: "Copy to clipboard" });
    });
  });

  //////////////////////// Prevent closing from click inside dropdown

  ///////////////////////////////////////

  /* ///////////////////////////////////////

    THESE FOLLOWING SCRIPTS ONLY FOR BASIC USAGE, 
    For sliders, interactions and other

    */ $(
    document
  ).on("click", ".dropdown-menu", function(e) {
    e.stopPropagation();
  });

  //////////////////////// Bootstrap tooltip
  if ($('[data-toggle="tooltip"]').length > 0) {
    // check if element exists
    $('[data-toggle="tooltip"]').tooltip();
  } // end if
});
// jquery end
