/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
const handleResult = (resultData) => {

    console.log("handleResult: populating genre table from resultData");

    // Populate genres
    const genresElement = jQuery("#genre-list");

    const genreCount = resultData.length;

    let row = "";
    for (let i = 0; i < genreCount; ++i) {
        if (i % 5 === 0) {
            row += "<tr>";
        }
        console.log(resultData[i]["genre-name"]);
        row +=
            "<td>" +
            "<a href='movies.html?genre=" + resultData[i]["genre-id"] + "'>" +
            resultData[i]["genre-name"] +
            "</a>" +
            "</td>";

        if (i % 5 === 4 || i === genreCount - 1) {
            row += "</tr>";
        }
    }

    genresElement.append(row);

    // Populate alphabets
    console.log("handleResult: populating alphabetical list");

    const alphamericElement = jQuery("#alphameric-list");

    row = "<tr id='alphabetical-list'>";
    for (let i = 0; i < 26; ++i) {
        row +=
            "<td>" +
            "<a href='movies.html?start-with=" + (String.fromCharCode('a'.charCodeAt(0) + i)) + "'>"
            + (String.fromCharCode('A'.charCodeAt(0) + i)) +
            "</a>" +
            " " +
            "</td>"
    }
    row += "</tr>";

    // alphamericElement.append(row);

    // Populate numbers
    console.log("handleResult: populating numerical list");

    // const numericalElement = jQuery("#numerical-list");

    row += "<tr id='numerical-list'>";
    for (let i = 0; i < 10; ++i) {
        row +=
            "<td>" +
            "<a href='movies.html?start-with=" + i + "'>"
            + i +
            "</a>" +
            " " +
            "</td>"
    }
    row +=
        "<td>" +
        "<a href='movies.html?start-with=" + "*" + "'>" +
        "*" +
        "</a>" +
        "</td>";
    row += "</tr>";

    alphamericElement.append(row);
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
 * Once this .js is loaded, following scripts will be executed by the browser
 */

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "../api/index", // Setting request url, which is mapped by IndexServlet in IndexServlet.java
    success: (resultData) => handleResult(resultData) // Setting callback function to handle data returned successfully by the IndexServlet
});