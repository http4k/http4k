// toggle search box via command + k
$(window).keydown(function(event) {
    if (event.metaKey && (event.keyCode === 75)) {
        event.preventDefault()
        $("#search").focus();
    }
});
