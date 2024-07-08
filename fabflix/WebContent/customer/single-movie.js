/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs three steps:
 *      1. Get parameter from request URL so it know which id to look for
 *      2. Use jQuery to talk to backend API to get the json data.
 *      3. Populate the data to correct html elements.
 */


/**
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
function getParameterByName(target) {
    let url = window.location.href;
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
function handleResult(resultData) {
    console.log("handleResult: populating movie info from resultData");

    // Populate the movie info
    let movieInfoElement = jQuery("#movie_info");

    movieInfoElement.append("<p>Title: " + resultData[0]["movie_title"] + "</p>" +
        "<p>Year: " + resultData[0]["movie_year"] + "</p>" +
        "<p>Director: " + resultData[0]["movie_director"] + "</p>" +
        "<p>Rating: " + resultData[0]["movie_rating"] + "</p>");

    // Populate genres
    console.log("handleResult: populating genres from resultData");

    let genresElement = jQuery("#movie_genres");
    let genres = "";

    for (let i = 0; i < resultData[0]["genre_id"].length; i++) {

        let gid = resultData[0]["genre_id"][i];
        let gname = resultData[0]["genre_name"][i];
        genres += "<a href='movies.html?genre=" + gid + "'>" + gname + "</a>";

        if (i < resultData[0]["genre_id"].length - 1) {

            genres += ", ";
        }
    }

    genresElement.append(genres);

    const actionButton = jQuery("button.action-btn");
    const action = "add";

    actionButton.attr("data-id", movieId);
    actionButton.attr("data-action", action);

    // Populate stars
    console.log("handleResult: populating star table from resultData");

    let starsElement = jQuery("#movie_stars");

    let starsRow = "<tr>";

    for (let i = 0; i < resultData[0]["star_id"].length; i++) {
        let sid = resultData[0]["star_id"][i];
        let sname = resultData[0]["star_name"][i];
        // Create a new row for each star
        let starRow = "<tr><td><a href='single-star.html?id=" + sid + "'>" + sname + "</a></td></tr>";
        // Append the new row to the table
        starsElement.append(starRow);
    }
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

jQuery(document).on("click", "button.action-btn", function (event) {

    event.preventDefault();

    const action = jQuery(this).data("action");

    console.log("Add movie to cart");

    jQuery.ajax({
        data: {
            "movie-id": movieId,
            "action": action
        },
        dataType: "json", // Setting return data type
        method: "GET", // Setting request method
        url: "../api/shopping-cart", // Setting request url, which is mapped by ShoppingCartServlet in ShoppingCartServlet.java
        success: (resultData) => handleResult(resultData) // Setting callback function to handle data returned successfully by the ShoppingCartServlet
    })
})

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


// Get movieId from URL
let movieId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "../api/single-movie?id=" + movieId,
    success: (resultData) => handleResult(resultData)
});