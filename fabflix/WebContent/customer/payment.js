let paymentForm = $("#payment-form");

/**
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
const getParameterByName = (target) => {
    let url = window.location.href;
    console.log(url);
    target = target.replace(/[\[\]]/g, "\\$&");

    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    return decodeURIComponent(results[2].replace(/\+/g, " "));
}


/**
 * Handle the data returned by PaymentServlet
 * @param resultDataString jsonObject
 */
function handlePaymentResult(resultDataString) {
    let resultDataJson = (typeof resultDataString === "string") ? JSON.parse(resultDataString) : resultDataString;

    console.log("handle payment response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);

    // If payment succeeds, display a success message and redirect after a delay
    if (resultDataJson["status"] === "success") {
        $("#payment_error_message").text("Payment successful! Redirecting...").css("color", "green").css("font-weight", "bold").show();

        // Redirect after a few seconds (e.g., 3 seconds)
        setTimeout(function() {
            window.location.replace("movies.html");
        }, 3000); // 3000 milliseconds = 3 seconds
    } else {
        // If payment fails, display error messages on <div> with id "payment_error_message"
        console.log("show error message");
        console.log(resultDataJson["message"]);
        $("#payment_error_message").text(resultDataJson["message"]).css("color", "red").css("font-weight", "bold").show();
    }
}


function handleError(jqXHR, textStatus, errorThrown) {
    // Log that the handleError function was called
    console.log("handleError function called");

    // Parse the error message from the server's response
    let errorMsg = JSON.parse(jqXHR.responseText);

    // Log the status and message to the console
    console.log("Status:", errorMsg["status"]);
    console.log("Message:", errorMsg["message"]);

    // Display the error message on the web page
    jQuery("#payment_error_message").text(errorMsg["message"]).css("color", "red").css("font-weight", "bold").show();
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitPaymentForm(formSubmitEvent) {
    console.log("submit payment form");

    formSubmitEvent.preventDefault();

    $.ajax(
        "../api/payment", {
            method: "POST",
            // Serialize the payment form to the data sent by POST request
            data: paymentForm.serialize(),
            success: (resulData) => handlePaymentResult(resulData),
            error: (err) => handleError(err)
        }
    );
}

/**
 * Once the movies button is clicked, following scripts will be executed by the browser
 */
jQuery("#movies-button").click((event) => {

    event.preventDefault();

    let url = "movies.html?";

    url += ("search-title=" + jQuery("#search-title").val() + "&");
    url += ("search-year=" + jQuery("#search-year").val() + "&");
    url += ("search-director=" + jQuery("#search-director").val() + "&");
    url += ("search-star=" + jQuery("#search-star").val() + "&");
    url += ("movies-per-page=" + 25 + "&");
    url += ("page-number=" + 1 + "&");
    url += ("sort-by=" + "title-asc-rating-asc");

    window.location.href = url;
})


/**
 * Once the search button is clicked, following scripts will be executed by the browser
 */
jQuery("#search-form").submit((event) => {

    event.preventDefault();

    let url = "movies.html?";

    url += ("search-title=" + jQuery("#search-title").val() + "&");
    url += ("search-year=" + jQuery("#search-year").val() + "&");
    url += ("search-director=" + jQuery("#search-director").val() + "&");
    url += ("search-star=" + jQuery("#search-star").val() + "&");
    url += ("movies-per-page=" + 25 + "&");
    url += ("page-number=" + 1 + "&");
    url += ("sort-by=" + "title-asc-rating-asc");

    window.location.href = url;
});


jQuery("#full-text-search-form").submit((event) => {
    event.preventDefault();

    let url = "movies.html?";

    url += ("full-text-search=" + jQuery("#full-text-search-title").val() + "&");

    url += ("movies-per-page=" + 25 + "&");
    url += ("page-number=" + 1 + "&");
    url += ("sort-by=" + "title-asc-rating-asc");

    console.log(url)
    console.log("Calling full text search")

    window.location.href = url;
});


const totalPrice = getParameterByName("total-price");
let items = JSON.parse(localStorage.getItem('cartItems'));
jQuery("#totalPrice").text(totalPrice);


// Bind the submit action of the form to a handler function
paymentForm.submit(submitPaymentForm);