/**
 * Handles the metadata returned by the DashBoard, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
const handleMetaDataResult = (resultData) => {

    // const metaData = jQuery("#meta-data");
    const topRow = jQuery("#top-row");
    const midRow = jQuery("#mid-row");
    const bottomRow = jQuery("#bottom-row");
    const tableCount = resultData.length;
    const cardPerRow = 4;

    topRow.empty();
    midRow.empty();
    bottomRow.empty();

    topRow.append(addMetaData(resultData, 0, cardPerRow));
    midRow.append(addMetaData(resultData, cardPerRow, cardPerRow * 2));
    bottomRow.append(addMetaData(resultData, cardPerRow * 2, tableCount));
}

const addMetaData = (resultData, start, end) => {

    let row = ""

    for (let i = start; i < end; ++i) {

        row += "<div class='card text-center col' style='width: 12rem'>";
        row += "<h5 class='card-title'>" + resultData[i]["table-name"] + "</h5>"
        row += "<table>";
        row += "<thead><tr><th>Name</th><th>Type</th></tr></thead>";
        row += "<tbody>";
        const rowPerTable = resultData[i]["col-name"].length;
        for (let j = 0; j < rowPerTable; ++j) {
            row += "<tr>";
            row += "<td>" + resultData[i]["col-name"][j] + "</td>";
            row += "<td>" + resultData[i]["col-type"][j] + "</td>";
            row += "</tr>";
        }
        row += "</tbody>" + "</table>" + "</div>";
    }
    return row;
}

jQuery(document).ready(() => {

    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "../dashboard/employee"
    }).done((resultData) => {
        handleMetaDataResult(resultData);
    }).fail((error) => {
        console.log("Get Metadata AJAX GET call failed");
        console.log(error);
    });

    jQuery("#add-star-form").on('submit', (event) => {

        event.preventDefault();
        console.log("Adding star...");

        const starName = jQuery(event.currentTarget).find("#star-name").val();
        const birthYear = jQuery(event.currentTarget).find("#star-birth-year").val();

        jQuery.ajax({
            data: {
                "star-name": starName,
                "star-birth-year": birthYear
            },
            dataType: "json",
            method: "POST",
            url: "../dashboard/add-star",
            contentType: "application/x-www-form-urlencoded; charset=UTF-8"
        }).done((msg) => {
            console.log("Add Star AJAX POST call success");
        }).fail((error) => {
            console.log("Add Star AJAX POST call failed");
            console.log(error);
        }).always((msg) => {
            jQuery("#add-star-log").append(
                "\n" + msg["status"] + "! " +
                "Added star ID: " + msg["star-id"]);
        });
    });

    jQuery("#add-movie-form").on('submit', (event) => {

        event.preventDefault();
        console.log("Adding movie...");

        const movieTitle = jQuery(event.currentTarget).find("#movie-title").val();
        const movieYear = jQuery(event.currentTarget).find("#movie-year").val();
        const movieDirector = jQuery(event.currentTarget).find("#movie-director").val();
        const movieStar = jQuery(event.currentTarget).find("#movie-star").val();
        const starBirthYear = jQuery(event.currentTarget).find("#movie-star-birth-year").val();
        const genreName = jQuery(event.currentTarget).find("#genre-name").val();

        jQuery.ajax({
            data: {
                "movie-title": movieTitle,
                "movie-year": movieYear,
                "movie-director": movieDirector,
                "movie-star": movieStar,
                "star-birth-year": starBirthYear,
                "genre-name": genreName
            },
            dataType: "json",
            method: "POST",
            url: "../dashboard/add-movie",
            contentType: "application/x-www-form-urlencoded; charset=UTF-8"
        }).done((msg) => {
            console.log("Add Movie AJAX POST call success");
        }).fail((error) => {
            console.log("Add Movie AJAX POST call failed");
            console.log(error);
        }).always((msg) => {
            jQuery("#add-movie-log").append(
                "\n" + msg["status"] + "! " +
                (Number.parseInt(msg["status-code"]) === 1 ?
                    msg["message"] + "\n" +
                    "Movie ID: " + msg["movie-id"] +
                    "; Star ID: " + msg["star-id"] +
                    "; Genre ID: " + msg["genre-id"]
                    : msg["message"])
            )
        });
    });
});