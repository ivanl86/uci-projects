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
const getParameterByName = (target) => {
    // Get request URL
    const url = window.location.href;
    // Encode target parameter name to url encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Ues regular expression to find matched parameter value
    const regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
const handleResult = (resultData) => {

    console.log("handleResult: populating star info from resultData");

    // populate the star info h3
    // find the empty h3 body by id "star-info"
    const starInfoElement = jQuery("#star-info");

    // append two html <p> created to the h3 body, which will refresh the page
    starInfoElement.append("<p>Star Name: " + resultData[0]["star-name"] + "</p>" +
        "<p>Date Of Birth: " + resultData[0]["star-birth-year"] + "</p>");

    console.log("handleResult: populating star table from resultData");

    // Populate the star table
    // Find the empty table body by id "star-table-body"
    const starTableBodyElement = jQuery("#star-table-body");

    // Concatenate the html tags with resultData jsonObject to create table rows
    const movieCount = resultData[0]["movie-ids"].length;
    let row = "";
    for (let i = 0; i < movieCount; ++i) {
        row +=
            "<tr>" +
            "<th>" +
            "<a href='single-movie.html?id=" + resultData[0]["movie-ids"][i] + "'>" +
            resultData[0]["movie-titles"][i] +
            "</a>" +
            "</th>" +
            "</tr>";
    }

    starTableBodyElement.append(row);
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

/**
 * Once this .js is loaded, following scripts will be executed by the browser\
 */

// Get id from URL
const starId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    data: {
        "id": starId // query by starId
    },
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "../api/single-star?", // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleResult(resultData) // Setting callback function to handle data returned successfully by the SingleStarServlet
});