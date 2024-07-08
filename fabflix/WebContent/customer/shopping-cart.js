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
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
const handleCartResult = (resultData) => {

    console.log("handleCartResult: populating shopping cart table from resultData");

    const shoppingCartTableBodyElement = jQuery("#shopping-cart-table-body");
    const movieCount = resultData.length;

    totalPrice = 0;
    items = [];
    shoppingCartTableBodyElement.empty();

    for (let i = 0; i < movieCount; ++i) {

        const movieId = resultData[i]["movie-id"];
        const purchasePrice = resultData[i]["purchase-price"];
        const quantity = resultData[i]["quantity"];
        const increaseAction = "increase";
        const decreaseAction = "decrease";
        const deleteAction = "delete";
        totalPrice += purchasePrice * quantity;
        items.push({id: movieId, quantity: quantity})
        let row = "";
        row += "<tr>";
        row +=
            "<td>" +
            "<a href='single-movie.html?id=" + movieId + "'>" +
            resultData[i]["movie-title"] +
            "</a>" +
            "</td>";
        row += "<td>" + "<div class='row flex-nowrap'>" +
            "<button data-id='" + movieId + "' " +
            "data-action='" + decreaseAction + "' " +
            "class='action-btn page-link' " +
            ">-</button>" + "&nbsp;" +
            quantity + "&nbsp;" +
            "<button data-id='" + movieId + "' " +
            "data-action='" + increaseAction + "' " +
            "class='action-btn page-link' " +
            ">+</button>" +
            "</div>" + "</td>";
        row += "<td>" +
            "<button data-id='" + movieId + "' " +
            "data-action='" + deleteAction + "' " +
            "class='action-btn page-link' " +
            ">Delete</button>" +
            "</td>";
        row += "<td>" + parseFloat(purchasePrice).toFixed(2) + "</td>";
        row += "<td>" + (purchasePrice * parseInt(quantity)).toFixed(2) + "</td>";
        row += "</tr>";


        shoppingCartTableBodyElement.append(row);

    }

    console.log(items);
    jQuery("#total-price").text("$ " + totalPrice.toFixed(2));
}


const callAjaxGet = () => {
    jQuery.ajax({
        data: {
            "action": ""
        },
        dataType: "json",
        method: "GET",
        url: "../api/shopping-cart",
        success: (resultData) => handleCartResult(resultData)
    });
}

let totalPrice = 0;
let items = [];

callAjaxGet();


jQuery(document).ready(() => {

    /**
     * Makes the HTTP PUT request when the Action button is clicked
     * Action buttons are increase, decrease, and delete
     */
    jQuery(document).on('click', 'button.action-btn', function (event) {
        event.preventDefault();

        // 'this' now correctly refers to the button that was clicked
        const movieId = jQuery(this).data("id");
        const action = jQuery(this).data("action");

        console.log("movie id: " + movieId);

        jQuery.ajax({
            data: {
                "movie-id": movieId,
                "action": action
            },
            dataType: "json", // Setting return data type
            method: "GET", // Setting request method
            url: "../api/shopping-cart", // Setting request url, which is mapped by ShoppingCartServlet in ShoppingCartServlet.java
            success: (resultData) => handleCartResult(resultData) // Setting callback function to handle data returned successfully by the ShoppingCartServlet
        });
    });

    jQuery("#proceed-to-payment-button").on("click", (event) => {

        event.preventDefault();

        // Stores the items to localStorage and retrieve the items after went to payment.html
        localStorage.setItem('cartItems', JSON.stringify(items));
        window.location.href = "payment.html?" + "total-price=" + totalPrice.toFixed(2);
    });

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
})