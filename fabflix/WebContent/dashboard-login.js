// const loginForm = jQuery("#login-form");

/**
 * Handle the data returned by DashBoardLoginServlet
 * @param resultDataJsonObject jsonObject
 */
const handleEmployeeLoginResult = (resultDataJsonObject) => {

    const resultDataString = (typeof resultDataJsonObject === 'string') ? JSON.parse(resultDataJsonObject) : resultDataJsonObject;
    const status = resultDataString["status"];
    const message = resultDataString["message"];

    console.log("Handling employee login response...");
    console.log("Response status: " + status);
    console.log("Response message: " + message);

    // grecaptcha.reset();
    jQuery("#login-message").text(message).show();

    if (status === "success") {
        window.location.replace("employee/dashboard.html");
    }
}

jQuery(document).ready(() => {

    jQuery("#login-form").submit((event) => {

        event.preventDefault();

        console.log("Login info submitted");
        jQuery.ajax({

            data: jQuery(event.currentTarget).serialize(),
            dataType: "json",
            method: "POST",
            url: "_dashboard/login"
        }).done((resultData) => {
            handleEmployeeLoginResult(resultData);
        }).fail((error) => {
            handleEmployeeLoginResult(error)
            console.log("AJAX POST call failed");
            console.log(error);
        });
    })
})

