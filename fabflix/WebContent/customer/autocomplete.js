$(document).ready(function () {
    var titleCache = {};  // Cache to store previous queries and results

    function handleLookup(query, doneCallback) {
        console.log("autocomplete initiated for:", query);
        console.log("checking cache for past results");

        // Check cache first
        if (query in titleCache) {
            console.log("using cached results for:", query);
            doneCallback({ suggestions: titleCache[query] });
        } else {
            console.log("sending AJAX request to backend Java Servlet for:", query);
            jQuery.ajax({
                method: "GET",
                url: '../api/autocomplete?query=' + escape(query),
                success: function(data) {
                   handleLookupAjaxSuccess(data, query, doneCallback);
                },
                error: function(errorData) {
                    console.log("lookup ajax error for:", query);
                    console.log(errorData);
                }
            });
        }
    }

    function handleLookupAjaxSuccess(data, query, doneCallback) {
        console.log("lookup ajax successful");
        try {
            var jsonData;
            // Check if data is already an object (not a string that needs parsing)
            if (typeof data === "object") {
                jsonData = data; // No need to parse
            } else {
                jsonData = JSON.parse(data); // Parse as JSON
            }
            console.log(jsonData);
            titleCache[query] = jsonData;  // Cache the result
            doneCallback({ suggestions: jsonData });
        } catch (e) {
            console.error("Error parsing JSON:", e);
            console.error("Received data:", data);
        }
    }

    function handleSelectSuggestion(suggestion) {
        console.log("you selected " + suggestion.value + " with ID " + suggestion.data["movieID"]);
        // TODO: Jump to the specific result page based on the selected suggestion
        window.location.href = `single-movie.html?id=${suggestion.data["movieID"]}`;
    }

    $('#full-text-search-title').autocomplete({
        lookup: function (query, doneCallback) {
            handleLookup(query, doneCallback);
        },
        onSelect: function (suggestion) {
            handleSelectSuggestion(suggestion)
        },
        deferRequestBy: 300,  // Delay in ms before sending the query
        minChars: 3,  // Minimum number of characters required to trigger autocomplete
    });

    function handleNormalSearch(query) {
        console.log("doing normal search with query: " + query);
        // TODO: You should do normal search here
    }

    $('#full-text-search-title').keypress(function(event) {
        if (event.keyCode == 13) {  // keyCode 13 is the Enter key
            handleNormalSearch($('#full-text-search-title').val());
        }
    });

    // TODO: if you have a "search" button, you may want to bind the onClick event as well of that button
});
