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
const handleMovieResult = (resultData) => {

    console.log("handleMovieResult: populating movie table from resultData");

    const movieTableBodyElement = jQuery("#movie-table-body");
    const movieCount = resultData.length;

    movieTableBodyElement.empty();

    for (let i = 0; i < movieCount; ++i) {

        const action = "add";

        let row = "";
        row += "<tr>";
        row +=
            "<th>" +
            "<a href='single-movie.html?id=" + resultData[i]["movie-id"] + "'>" +
            resultData[i]["movie-title"] +
            "</a>" +
            "</th>";
        row += "<th>" + resultData[i]["movie-year"] + "</th>";
        row += "<th>" + resultData[i]["movie-director"] + "</th>";
        row += "<th>";
        const genreCount = resultData[i]["genre-ids"].length;
        for (let j = 0; j < genreCount; ++j) {
            row +=
                "<a href='movies.html?genre=" + resultData[i]["genre-ids"][j] + "'>" +
                resultData[i]["genre-names"][j] +
                "</a>";
            if (j < genreCount - 1) {
                row += ", ";
            }
        }
        row += "</th>";
        row += "<th>";
        const starCount = resultData[i]["star-ids"].length;
        for (let j = 0; j < starCount; ++j) {
            row +=
                "<a href='single-star.html?id=" + resultData[i]["star-ids"][j] + "'>" +
                resultData[i]["star-names"][j] +
                "</a>";
            if (j < starCount - 1) {
                row += ", ";
            }
        }
        row += "</th>";
        row += "<th>" + resultData[i]["movie-rating"] + "</th>"
        row += "<th>" +
                "<button data-id='" + resultData[i]["movie-id"] + "' " +
                "data-title='" + resultData[i]["movie-title"] + "' " +
                "data-action='" + action + "' " +
                "class='add-to-cart-btn page-link'>Add</button>" +
                "</th>";

        row += "</tr>";

        movieTableBodyElement.append(row);
    }
}

/**
 * Makes the HTTP GET request using JQuery ajax method
 */
const callAjaxGet = () => {
    jQuery.ajax({
        data: {
            "genre": genreId,
            "start-with": firstChar,
            "movies-per-page": moviesPerPage,
            "sort-by": sortBy,
            "page-number": pageNumber,
            "full-text-search": fullTextSearch,
            "search-title": searchTitle,
            "search-year": searchYear,
            "search-director": searchDirector,
            "search-star": searchStar
        },
        dataType: "json",
        method: "GET",
        url: "../api/movies?", // Adjust if necessary to point to the correct API endpoint
        success: (resultData) => handleMovieResult(resultData),
        error: function (jqXHR, textStatus, errorThrown) {
            console.error('Error occurred during the AJAX request', textStatus, errorThrown);
        }
    });
};

// const fullTextAjaxGet = () => {
//     console.log("Call the full text Ajax");
//     jQuery.ajax({
//         data: {
//             "genre": genreId,
//             "start-with": firstChar,
//             "movies-per-page": moviesPerPage,
//             "sort-by": sortBy,
//             "page-number": pageNumber,
//             "new-search-title": newSearchTitle
//         },
//         dataType: "json",
//         method: "GET",
//         url: "../api/full-text-search?", // Ensure this matches the servlet URL pattern exactly
//         success: (resultData) => handleMovieResult(resultData),
//         error: function (jqXHR, textStatus, errorThrown) {
//             console.error('Error occurred during the AJAX request', textStatus, errorThrown);
//         }
//     });
// };

/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */
let moviesPerPage = localStorage.getItem("movies-per-page") !== null ?
    localStorage.getItem("movies-per-page") : jQuery("#movies-per-page option:selected").val();
let pageNumber = localStorage.getItem("page-number") !== null ?
    localStorage.getItem("page-number") : 1;
let sortBy = localStorage.getItem("sort-by") !== null ?
    localStorage.getItem("sort-by") : jQuery("#sort-by option:selected").val();

const genreId = getParameterByName("genre");
const firstChar = getParameterByName("start-with");
const searchTitle = getParameterByName("search-title");
const searchYear = getParameterByName("search-year");
const searchDirector = getParameterByName("search-director");
const searchStar = getParameterByName("search-star");
const fullTextSearch = getParameterByName("full-text-search");

console.log("Genre ID: " + genreId);
console.log("First char: " + firstChar);
console.log("Search title: " + searchTitle);
console.log("Search year: " + searchYear);
console.log("Search director: " + searchDirector);
console.log("Search star: " + searchStar);
console.log("Full text search title: " + fullTextSearch);

localStorage.setItem("movies-per-page", moviesPerPage);
localStorage.setItem("page-number", pageNumber);
localStorage.setItem("sort-by", sortBy);

console.log("Start...");
console.log("Movies per page: " + localStorage.getItem("movies-per-page"));
console.log("Page number: " + localStorage.getItem("page-number"));
console.log("Sort by: " + localStorage.getItem("sort-by"));

callAjaxGet();

jQuery(document).ready(() => {
    const inputField = jQuery('#autocomplete-input');
    let timeout = null; // Timer to manage delay
    const resultsContainer = jQuery('#autocomplete-results');

    inputField.on('keyup', function() {
        clearTimeout(timeout); // Clear the existing timer on each key press
        const searchText = jQuery(this).val().trim();

        // Only start autocomplete if search text length is at least 3 characters
        if (searchText.length >= 3) {
            timeout = setTimeout(() => {
                fetchAutocompleteSuggestions(searchText);
            }, 300); // Set delay before making AJAX call
        } else {
            resultsContainer.empty(); // Clear results if search text is too short
        }
    });

    function fetchAutocompleteSuggestions(query) {
        jQuery.ajax({
            url: '../api/autocomplete', // Ensure this matches the correct URL
            method: 'GET',
            dataType: 'json',
            data: { query: query },
            success: function(data) {
                displayAutocompleteResults(data);
            },
            error: function() {
                console.error('Error fetching autocomplete suggestions');
            }
        });
    }

    function displayAutocompleteResults(results) {
        const resultsContainer = jQuery('#autocomplete-results');
        resultsContainer.empty(); // Clear previous results

        if (results.length > 0) {
            results.forEach(result => {
                const div = jQuery('<div>').addClass('autocomplete-suggestion')
                    .text(result.title)
                    .attr('data-id', result.id) // Store the movie ID in a data attribute
                    .on('click', function () {
                        const movieId = jQuery(this).attr('data-id');
                        window.location.href = `single-movie.html?id=${movieId}`; // Redirect to the movie detail page
                    })
                    .on('mouseenter', function () {
                        jQuery(this).siblings().removeClass('autocomplete-selected');
                        jQuery(this).addClass('autocomplete-selected');
                    })
                    .on('mouseleave', function () {
                        jQuery(this).removeClass('autocomplete-selected');
                    });
                resultsContainer.append(div);
            });
        } else {
            resultsContainer.append(jQuery('<div>').addClass('autocomplete-suggestion').text('No results found'));
        }
    }

        inputField.on('keydown', function(e) {
            let selected = jQuery('.autocomplete-selected');
            let items = jQuery('.autocomplete-suggestion');

            if (e.key === 'ArrowDown') {
                if (selected.length === 0) {
                    items.first().addClass('autocomplete-selected');
                } else {
                    var next = selected.removeClass('autocomplete-selected').next('.autocomplete-item');
                    if (next.length) {
                        next.addClass('autocomplete-selected');
                    } else {
                        items.first().addClass('autocomplete-selected');
                    }
                }
                e.preventDefault(); // Prevent the cursor from moving in the input field
            } else if (e.key === 'ArrowUp') {
                if (selected.length === 0) {
                    items.last().addClass('autocomplete-selected');
                } else {
                    var prev = selected.removeClass('autocomplete-selected').prev('.autocomplete-item');
                    if (prev.length) {
                        prev.addClass('autocomplete-selected');
                    } else {
                        items.last().addClass('autocomplete-selected');
                    }
                }
                e.preventDefault(); // Prevent the cursor from moving in the input field
            } else if (e.key === 'Enter') {
                if (selected.length) {
                    selected.click();
                }
                e.preventDefault(); // Prevent form submission
            }
        });

        // Hide results when clicking outside
    jQuery(document).on('click', function(event) {
        if (!jQuery(event.target).closest('#autocomplete-input, #autocomplete-results').length) {
            resultsContainer.empty();
        }
    });

    /**
     * Once the movies button is clicked, following scripts will be executed by the browser
     */
    jQuery("#movies-button").click((event) => {

        event.preventDefault();

        window.location.href = "movies.html?";
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
        callAjaxGet();
    });

    /**
     * Once the full-text search form is submitted
     */
    jQuery("#full-text-search-form").submit((event) => {
        event.preventDefault();

        let url = "movies.html?";

        url += ("full-text-search=" + jQuery("#full-text-search-title").val() + "&");

        url += ("movies-per-page=" + 25 + "&");
        url += ("page-number=" + 1 + "&");
        url += ("sort-by=" + "title-asc-rating-asc");

        console.log(url)
        console.log("Calling full text search")

        // setTimeout((event) => {
        //
        // }, 3000);

        window.location.href = url;
        callAjaxGet();
    });

    /**
     * Makes the HTTP GET request when the sorting option is submitted
     */
    jQuery("#display-option-form").submit((event) => {

        event.preventDefault();

        moviesPerPage = $("#movies-per-page option:selected").val();
        pageNumber = JSON.parse(localStorage.getItem("page-number"));
        sortBy = jQuery("#sort-by option:selected").val();

        while (moviesPerPage * pageNumber > 100) {

            --pageNumber;
        }

        localStorage.setItem("movies-per-page", moviesPerPage);
        localStorage.setItem("page-number", pageNumber)
        localStorage.setItem("sort-by", sortBy);

        callAjaxGet();
    });


    /**
     * Makes the HTTP GET request when the previous button is clicked
     */
    jQuery("#previous-btn").click((event) => {

        event.preventDefault();
        if (pageNumber <= 1) {
            return;
        }

        --pageNumber;

        callAjaxGet();
    });

    /**
     * Makes the HTTP GET request when the next button is clicked
     */
    jQuery("#next-btn").click((event) => {

        event.preventDefault();
        if (moviesPerPage * pageNumber >= 100) {
            return;
        } // 10 -> 10, 25 -> 4, 50 -> 2, 100 -> 1

        ++pageNumber;

        callAjaxGet();
    });


    /**
     * Makes the HTTP GET request when the Add button is clicked
     */
// Use the document or a static parent element to delegate the event
    jQuery(document).on('click', 'button.add-to-cart-btn', function(event) {

        event.preventDefault();

        // 'this' now correctly refers to the button that was clicked
        const movieId = jQuery(this).data("id");
        const movieTitle = jQuery(this).data("title");
        const action = jQuery(this).data("action");
        console.log("movie id: " + movieId);
        console.log("action: " + action);

        jQuery.ajax({
            data: {
                "movie-id": movieId,
                "action": action
            },
            dataType: "json", // Setting return data type
            method: "GET", // Setting request method
            url: "../api/shopping-cart", // Setting request url, which is mapped by ShoppingCartServlet in ShoppingCartServlet.java
            success: window.alert(movieTitle + " is added to cart!") // Setting callback function to handle data returned successfully by the ShoppingCartServlet
        });
    });
})
