/*
List of all Global Variables and user
- domain: holds all the domains retrueved from the GET request.
- radius: Defines the radius of the circle for nodes.
- types: unused here
- patialGraph: the variable holds the partialgraph that gets sent to the backend with the POST request
- rejectedGraph: the variable holds the rejected graph and gets updated everytime a node or an edge gets deleted. Also gets updated by unselected suggestions
- disconnectedGraph: The variable is true: if the graph is disconnected and false: if the the graph is connected.
- suggestionCounter:
- suggestionPicked: Suggestion picked is set to true when the user has selected a suggestion from the suggested noodes(grey nodes).
- nextButtonClick: this variable is set to true everytime the user asks for the next set of suggestions by clicking the 'Try Different Suggestions' button.
- defaultNodeColor: represents the color of an unselected node.
- selectedNodeColer: represents the color the node is set to if selected.
- nodeclick: the variable captures a node click. Id set to true if a node is clicked.
- selectedlinkColo: not implemented
- allowNodeCreation: the variable is set to true in a case when we want to allow node creation. It is set to false if the user clicks a link or a node, or of the shift key is held down
- newNode: The variable is set to true everytime a new variable gets created.
- value: unused
- drawedge: set to true when the user is allowed to draw an edge. Set to false when the shift key is held down
- min_zoom, max_zoom: used to set limit to the zoom when feature was included. currently unused.
- suggestions: set to true when suggested nodes(grey nodes) are still being displayed to the user.
*/

var sessionId = getUniqueId();
var urlFull = window.location.host;
var urlSuffix = window.location.pathname.split('/')[1];
var systemName = urlSuffix.startsWith('orion-n') ? 'naive' : (urlSuffix.startsWith('orion-b') ? 'baseline' : (urlSuffix.startsWith('orion-h') ? 'hybrid' : 'orion'));
var version = urlSuffix.replace('orion-n', '').replace('orion-b', '').replace('orion-h', '').replace('orion', ''); //user study query set
var editorPage = window.location.href;
var partialGraph = [];
var rejectedGraph = [];
var linkTypes = [];
var returnObject = [];
var allReturnObject = [];
var disconnectedGraph = false;
var noOfSuggestions = 3;
var suggestionCounter = 0;
var noOfSuggestionsPicked = 0;
var suggestionsFinalized = false;
var suggestionPicked = false;
var nextButtonClick = false;
var subSuggestionsInProgressFlag = false;
var notnull = false;
var defaultNodeColor = "#FF6600",
    selectedNodeColor = "#FFCC99",
    nodeclick = false,
    selectedlinkColor = "red",
    allowNodeCreation = true,
    newNode = false,
    newEdge = false;
// set up SVG for D3
// var defaultNodeColor = "#FF8000", selectedNodeColor = "#FFCC99", nodeclick = false, selectedlinkColor = "red", allowNodeCreation = true, newNode = false, newEdge = false;
// var types = ["Select name","type1", "type2", "type3", "type4", "type5", "type6", "type7"];
// var domain = ["Select Name", "Domain1", "Domain2", "domain3", "Domain4", "Domain5", "Domain6"];
// var linkTypes = ["Select name", "name1", "name2", "name3", "name4", "name5",  "name6" , "name7"];
var value = [],
    selected_edge = null;
// var width  = jQuery("#app-body").width(),
//     height = jQuery("#app-body").height(),
var width = jQuery("#app-body").width(),
    height = 600,
    colors = d3.scale.category10();
var radius = 15;
var drawEdge = true;
var min_zoom = 0.1;
var max_zoom = 7;
var zoom = d3.behavior.zoom().scaleExtent([min_zoom, max_zoom]);
var suggestions = false;
var greyLinkSelectedFlag = 0;
var greyLinkSelected = null;
var greyLinkSuggestionsPicked = 0;
var SugPickdPerGreyLinkCount = 0;
var greyLinkPickdPerSubSuggestion = 0;
var linkSuggestionsFinalized = false;
var greyLinkDetails = [];
var exampleReverseRoleFlag = false;
var exampleLinkGraphId;
var exampleSource;
var exampleTarget;
var exampleLinkId;
var exampleLinkValue;
var exampleActualSourceType;
var exampleActualTargetType;
var response = []; // Example Query Response
var responseExample = [];
var checkMove_x = 0;
var checkMove_y = 0;
var hideDragLineFlag = false;
var addEdgeButtonEdgeAdded = false;
var waitForJsonResultFlag = 0;
var windowSize = 100;
var nodeEditList = [];
var nodeEditEdges = [];
var nodeEditWindowNo = 0;
var nodeEditTypeWindowNo = 0;
var nodeToBeEdited;
var editNodeKeyword;
var editList = [];
var selectNodeModalClosed = false;
var mouseOverFlag = false;
var mouseOverEdgeFlag = false;
var showInstructionsFlag = 0;
var refreshSelectedNode = 0;
var suggestion_mode = 0;
var usefulTipsFlag = 0;
var settingsFlag = 0;
var edgeTypesFlag = 0;
var clearButtonFlag = 0;
var no_Of_Iterations = 0;
var timeInitialisationFlag = 0;
var entityDefaultSize = 20;
var forLinks = 0;
var undo_links = [];
var undo_nodes = [];
var redo_links = [];
var redo_nodes = [];
var mouseOnOption = false;
var alpha = 0.1;
var curTypeList = [];
var curEndTypeList = [];
//var tdListText = "<b>List of (Domain:Type) the node belongs to: </b>";
var tdListText = "This node currently belongs to the following Types (and the corresponding Domains):";
var deletedTypes = [];
var prevDomSel = null;
var prevTypSel = null;
var prevEntSel = null;

var allDomains = [];
var allTypes = [];
var allEntities = [];
var domains = [];
var types = [];
var entities = ["Select entity", "entity1", "entity2", "entity3", "entity4", "entity5", "entity6"];
var typesThroughDomain = [];
var entitiesThroughDomain = [];
var entitiesThroughType = [];
var generatedDomains = [];
var generatedTypes = [];
var generatedEntities = [];
var curNodeLabel = null;
var endTypesForEdgeMap = new Map(); //saves a precalculated end types of an edge in memory
var domainForTypeMap = new Map(); //saves a precalculated domain of a type in memory
var queryWindowNum = 1;
var queryWindowSize = 5;
var queryKeyword = "";

var eq = [
    //"Find all <b>ACTOR & DIRECTOR</b>s who studied in <b>Harvard University</b>, the <b>FILM</b>s they acted in, and their <b>AWARD NOMINATED WORK</b>s",
    "What <b>FILM</b> was acted by <b>Taylor Lautner</b> and edited by <b>Christopher Greenbury</b>?",
    "What <b>UNIVERSITY</b> did <b>Barack Obama</b> attend?",
    "<b>David Gilmour</b> and <b>Roger Waters</b> were members of which <b>MUSICAL GROUP</b>?",
    "<b>Dublin</b> is the capital of which <b>COUNTRY</b>?",
    "Who is the <b>CREATOR</b> of the comic book series <b>Watchmen</b> and what <b>AWARD CATEGORY</b> did they win?"
];

var mq = [
    "Find the <b>GAME DESIGNER</b> who worked in <b>ion Storm</b> and designed a <b>GAME</b> that was published by <b>Steve Jackson Games</b>.",
    "Find <b>AUTHORS</b> who wrote <b>BOOKS</b> on <b>Middle Ages</b> in <b>English language</b> and also directed <b>FILM</b>s.",
    "Which <b>American MARTIAL ARTIST</b> won medal in <b>1960 Summer Olympics</b> and which <b>SPORT</b> did they play?",
    "Which <b>MILITARY COMMANDER</b> died in <b>London</b> and commanded in a <b>MILITARY CONFLICT</b> that happened in the same <b>LOCATION</b> where <b>Eiffel Tower</b> is located?",
    "Find all <b>BASKETBALL PLAYER</b>s who played on <b>BASKETBALL TEAM</b>s located in the same <b>LOCATION</b> the player was born in."
];

var hq = [
    "Who was the <b>CREATOR</b> of the <b>FICTIONAL UNIVERSE</b> that had a character named <b>Gimli</b> of character species <b>Dwarf</b>, authored the book <b>The Silmarillion</b> and graduated from <b>University of Oxford</b>?",
    "Find all <b>COUNTRIES</b>, their <b>CURRENCY</b> and their <b>TOURIST ATTRACTION</b>s; where the <b>official language</b> of the country is under the language family <b>Indo-European Languages</b> and the writing system of the language is <b>Latin alphabet</b>. ",
    "Find an <b>American CHEF</b> who authored a <b>RECIPE</b> for a <b>DISH</b> that belongs to <b>French food</b> cuisine and the chef is associated with the restaurant <b>The French Laundry</b>.",
    "Find the <b>ORGANIZATION FOUNDER</b> of the <b>AIRCRAFT MANUFACTURER</b> that designed aircraft model <b>SpaceShipTwo</b> that has the aircraft type <b>Spaceplane</b>. Also find the <b>AWARD</b>s the founder has won and the <b>ARMED FORCE</b>s he served in.",
    "Find the <b>POLITICAL IDEOLOGY</b> of a <b>POLITICAL PARTY</b> that is located in a <b>COUNTRY</b> whose national anthem is <b>Marcha Real</b>, under the condition that author <b>Jung Chang</b> won an award for a <b>BOOK</b> that is based on that political ideology."
];

function updateUndo() {
    jQuery("#undo-button").attr('class', 'menu-border');
    jQuery("#undo-text").text("Undo");
    jQuery("#undo-icon").text("undo");

    undo_nodes.length = 0;
    for (var i = 0; i < nodes.length; i++) {
        var n = {
            id: nodes[i].id,
            name: nodes[i].name,
            flag: nodes[i].flag,
            greyflag: nodes[i].greyflag,
            nodeID: nodes[i].nodeID,
            domain: nodes[i].domain,
            type: nodes[i].type,
            entity: nodes[i].entity,
            tempName: nodes[i].tempName,
            x: nodes[i].x,
            y: nodes[i].y,
            px: nodes[i].px,
            py: nodes[i].py,
            index: nodes[i].index,
            weight: nodes[i].weight
        };
        undo_nodes.push(n);
    }
    undo_links.length = 0;
    for (var i = 0; i < links.length; i++) {
        var l = {
            source: links[i].source,
            target: links[i].target,
            value: links[i].value,
            id: links[i].id,
            flag: links[i].flag,
            linkID: links[i].linkID,
            linkNum: links[i].linkNum,
            actualSourceType: links[i].actualSourceType,
            actualTargetType: links[i].actualTargetType
        };
        undo_links.push(l);
    }

}

function updateRedo() {

    redo_nodes.length = 0;
    for (var i = 0; i < nodes.length; i++) {
        var n = {
            id: nodes[i].id,
            name: nodes[i].name,
            flag: nodes[i].flag,
            greyflag: nodes[i].greyflag,
            nodeID: nodes[i].nodeID,
            domain: nodes[i].domain,
            type: nodes[i].type,
            entity: nodes[i].entity,
            tempName: nodes[i].tempName,
            x: nodes[i].x,
            y: nodes[i].y,
            px: nodes[i].px,
            py: nodes[i].py,
            index: nodes[i].index,
            weight: nodes[i].weight
        };
        redo_nodes.push(n);
    }
    redo_links.length = 0;
    for (var i = 0; i < links.length; i++) {
        var l = {
            source: links[i].source,
            target: links[i].target,
            value: links[i].value,
            id: links[i].id,
            flag: links[i].flag,
            linkID: links[i].linkID,
            linkNum: links[i].linkNum,
            actualSourceType: links[i].actualSourceType,
            actualTargetType: links[i].actualTargetType
        };
        redo_links.push(l);
    }

}


$(document).bind("contextmenu", function(e) {
    return false;
});

jQuery("#lower-div-2").height(temp - 296);
//This function adds the animation to the active help.
jQuery('#active-help').each(function() {
    jQuery(this).children().each(function(i) {
        jQuery(this).delay((i++) * 2000).animate({
            left: 0,
            opacity: 1
        });
    });
});

//Set the height of the svg graph.
// jQuery("#app-body").attr("height",(screen.height*.6));
jQuery("#svg-div").attr("height", "400");

jQuery("#declinePolicy").click(function() {
    window.location = "https://"+urlFull+"/"+version;
});

jQuery(document).ready(function() {
    if(version !== "") {
        jQuery("#query-box").show();
        jQuery("#submit-button").show();
    }

    if(systemName !== "naive") {
        jQuery("#settings-div").show();
        jQuery("#button-g").show();
    }

    jQuery("#selectDiv").hide();
    //Initial GET request to get all Domain suggestions.
    getAllDomains(allDomains);
    getAllTypes(allTypes);
    getAllEntities(allEntities);

    jQuery.ajax({
        url: "https://" + urlFull + "/orion_app/clearcanvas?sessionid=" + sessionId,
        type: 'get',
        async: false,
        success: function(response) {}
    });

    jQuery("#wrap1").toggleClass('active');
    jQuery("#possible-actions").toggleClass('active');

    //jQuery("#wrap2").toggleClass('active');
    //jQuery("#useful-tips").toggleClass('active');
    //usefulTipsFlag = 1;


    var name = "policy=";
    var decodedCookie = decodeURIComponent(document.cookie);
    var fl = false;
    var ca = decodedCookie.split(';');
    for (var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            fl = true;

        }
    }
    if (!fl) {
        jQuery("#policyModal").modal('show');
    }
});

/*
Function for the 'x' button of the options pane. It deletes the node or the edge, if the 'x' is clicked instead of the 'save' button.
*/
jQuery("#x-button").click(function() {
    checkKeywordStatus();
    if(nodeToBeEdited == null) {
      if (selected_node) {
          nodes.splice(nodes.indexOf(selected_node), 1);
          jQuery("#show-instructions").attr("disabled", false);
          var graphDisconnect = checkConnected(nodes, links);
          if (nodes.length == 0) {
              jQuery("#button-g").attr("class", "inactive");
              jQuery("#submit-button").attr("class", "inactive");

          } else {
              jQuery("#button-g").attr("class", "menu-border");
              jQuery("#submit-button").attr("class", "menu-border");
          }
          selectNodeModalClosed = true;
          animatedHelp();
      } else if (selected_link) {
          selected_link.source = selected_link.target;
          selected_link = null;
          //     allowNodeDrag = true;
          //     translateAllowed = true;
          //     drawEdge = false;
          allowNodeCreation = false;
      };
      checkKeywordStatus();
      restart();
    } else {
      // jQuery("#node-edit-border").hide();
      // jQuery("#node-edit-entity").hide();
      // jQuery("#node-edit-entity-search").hide();
      // jQuery("#node-edit-type").hide();
      nodeToBeEdited = null;
    }

});


jQuery("#node-edit-x-button").click(function() {
    jQuery("#node-edit-border").hide();
    jQuery("#node-edit-entity").hide();
    jQuery("#node-edit-entity-search").hide();
    jQuery("#node-edit-type").hide();
    nodeToBeEdited = null;
    // checkKeywordStatus();
    //
    // restart();
    // refreshAllSuggestion();
});

function setQueryNo(cvalue) {

    document.cookie = "query_no=" + cvalue + ";";
}

function getUniqueId() {
    var name = "uniqid=";
    var decodedCookie = decodeURIComponent(document.cookie);
    var ca = decodedCookie.split(';');
    for (var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    var s = Math.random().toString(36).slice(2);
    document.cookie = "uniqid=" + s + ";";
    return s;



}


function onFormSubmit() {
    if (submitted) {
        switch (getQuery()) {
            case "1":
                setQueryNo("2");
                window.location = editorPage;
                break;
            case "2":
                setQueryNo("3");
                window.location = editorPage;
                break;
            case "3":
                setQueryNo("4");
                window.location = editorPage;
                break;
            case "4":
                setQueryNo("5");
                window.location = editorPage;
                break;
            case "5":
                setQueryNo("6");
                window.location = editorPage;
                break;
            case "6":
                setQueryNo("7");
                jQuery("#surveyForm").modal("hide");
                jQuery("#FinalSurveyForm").modal("show");

                break;
        }
    }
}
jQuery("#finalCloseButton").click(function() {
    window.location = editorPage;

})

function onFinalFormSubmit() {

    if (submitted) {
        document.cookie = "uniqid=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        document.cookie = "query_no=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        jQuery("#FinalSurveyForm").modal('hide');
        jQuery("#finalThankYouModal").modal('show');


    }
}

function myFunctionForHiding() {
    document.cookie = "policy=accepted;";
    jQuery('#policyModal').modal('hide');
    //  document.getElementById('tobeHiddenAfterClick').innerHTML = document.getElementById('IntroPart').innerHTML;
    //  document.getElementById('initial-read').style.display = "block";

}

function getQuery() {
    var name = "query_no=";
    var decodedCookie = decodeURIComponent(document.cookie);
    var ca = decodedCookie.split(';');
    for (var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "1";
}

switch (getQuery()) {
    case "1":
        jQuery("#query-header").text("Query 1");
        if(version === "1") jQuery("#query-desc").html(eq[0]);
        if(version === "2") jQuery("#query-desc").html(eq[0]);
        if(version === "3") jQuery("#query-desc").html(eq[0]);
        if(version === "4") jQuery("#query-desc").html(eq[0]);
        if(version === "5") jQuery("#query-desc").html(eq[1]);
        if(version === "6") jQuery("#query-desc").html(eq[1]);
        if(version === "7") jQuery("#query-desc").html(eq[1]);
        if(version === "8") jQuery("#query-desc").html(eq[2]);
        if(version === "9") jQuery("#query-desc").html(eq[2]);
        if(version === "10") jQuery("#query-desc").html(eq[3]);
        //jQuery("#query-desc").html("Find all <b>architects</b> who studied in the <b>University of Oxford</b>.");
        document.getElementById("entry_31427235").value = "1";
        break;
    case "2":
        jQuery("#query-header").text("Query 2");
        if(version === "1") jQuery("#query-desc").html(eq[1]);
        if(version === "2") jQuery("#query-desc").html(eq[2]);
        if(version === "3") jQuery("#query-desc").html(eq[3]);
        if(version === "4") jQuery("#query-desc").html(eq[4]);
        if(version === "5") jQuery("#query-desc").html(eq[2]);
        if(version === "6") jQuery("#query-desc").html(eq[3]);
        if(version === "7") jQuery("#query-desc").html(eq[4]);
        if(version === "8") jQuery("#query-desc").html(eq[3]);
        if(version === "9") jQuery("#query-desc").html(eq[4]);
        if(version === "10") jQuery("#query-desc").html(eq[4]);
        //jQuery("#query-desc").html("Find all <b>Indian</b> (nationality) <b>actors</b>, and the <b>films</b> they acted in.");
        document.getElementById("entry_31427235").value = "2";
        break;
    case "3":
        jQuery("#query-header").text("Query 3");
        if(version === "1") jQuery("#query-desc").html(mq[0]);
        if(version === "2") jQuery("#query-desc").html(mq[0]);
        if(version === "3") jQuery("#query-desc").html(mq[0]);
        if(version === "4") jQuery("#query-desc").html(mq[0]);
        if(version === "5") jQuery("#query-desc").html(mq[1]);
        if(version === "6") jQuery("#query-desc").html(mq[1]);
        if(version === "7") jQuery("#query-desc").html(mq[1]);
        if(version === "8") jQuery("#query-desc").html(mq[2]);
        if(version === "9") jQuery("#query-desc").html(mq[2]);
        if(version === "10") jQuery("#query-desc").html(mq[3]);
        //jQuery("#query-desc").html("Find all <b>basketball players</b> who were head-coach of <b>Los Angeles Lakers</b>, and their parents’ name.");
        document.getElementById("entry_31427235").value = "3";
        break;
    case "4":
        jQuery("#query-header").text("Query 4");
        if(version === "1") jQuery("#query-desc").html(mq[1]);
        if(version === "2") jQuery("#query-desc").html(mq[2]);
        if(version === "3") jQuery("#query-desc").html(mq[3]);
        if(version === "4") jQuery("#query-desc").html(mq[4]);
        if(version === "5") jQuery("#query-desc").html(mq[2]);
        if(version === "6") jQuery("#query-desc").html(mq[3]);
        if(version === "7") jQuery("#query-desc").html(mq[4]);
        if(version === "8") jQuery("#query-desc").html(mq[3]);
        if(version === "9") jQuery("#query-desc").html(mq[4]);
        if(version === "10") jQuery("#query-desc").html(mq[4]);
        //jQuery("#query-desc").html("Find the <b>symptoms, vaccine name</b> and vaccine inventor of <b>Hepatitis E</b>.");
        document.getElementById("entry_31427235").value = "4";
        break;
    case "5":
        jQuery("#query-header").text("Query 5");
        if(version === "1") jQuery("#query-desc").html(hq[0]);
        if(version === "2") jQuery("#query-desc").html(hq[0]);
        if(version === "3") jQuery("#query-desc").html(hq[0]);
        if(version === "4") jQuery("#query-desc").html(hq[0]);
        if(version === "5") jQuery("#query-desc").html(hq[1]);
        if(version === "6") jQuery("#query-desc").html(hq[1]);
        if(version === "7") jQuery("#query-desc").html(hq[1]);
        if(version === "8") jQuery("#query-desc").html(hq[2]);
        if(version === "9") jQuery("#query-desc").html(hq[2]);
        if(version === "10") jQuery("#query-desc").html(hq[3]);
        //jQuery("#query-desc").html("Find all <b>books</b> on <b>Socialism</b> written in <b>English Language</b>, their <b>author</b> name, and the name of <b>films</b> which were directed by the author.");
        document.getElementById("entry_31427235").value = "5";
        break;
    case "6":
        jQuery("#query-header").text("Query 6");
        if(version === "1") jQuery("#query-desc").html(hq[1]);
        if(version === "2") jQuery("#query-desc").html(hq[2]);
        if(version === "3") jQuery("#query-desc").html(hq[3]);
        if(version === "4") jQuery("#query-desc").html(hq[4]);
        if(version === "5") jQuery("#query-desc").html(hq[2]);
        if(version === "6") jQuery("#query-desc").html(hq[3]);
        if(version === "7") jQuery("#query-desc").html(hq[4]);
        if(version === "8") jQuery("#query-desc").html(hq[3]);
        if(version === "9") jQuery("#query-desc").html(hq[4]);
        if(version === "10") jQuery("#query-desc").html(hq[4]);
        //jQuery("#query-desc").html("Find all <b>asteroids</b> of <b>Centaur</b> asteroid group and the <b>astronomer</b> that discovered it, where the astronomer was born in <b>United States of America</b>. Also find the <b>space missions</b> where the asteroid was the destination.");
        document.getElementById("entry_31427235").value = "6";
        break;
    default:
        setQueryNo("1");
        jQuery("#query-header").text("Query 1");
        if(version === "1") jQuery("#query-desc").html(eq[0]);
        if(version === "2") jQuery("#query-desc").html(eq[0]);
        if(version === "3") jQuery("#query-desc").html(eq[0]);
        if(version === "4") jQuery("#query-desc").html(eq[0]);
        if(version === "5") jQuery("#query-desc").html(eq[1]);
        if(version === "6") jQuery("#query-desc").html(eq[1]);
        if(version === "7") jQuery("#query-desc").html(eq[1]);
        if(version === "8") jQuery("#query-desc").html(eq[2]);
        if(version === "9") jQuery("#query-desc").html(eq[2]);
        if(version === "10") jQuery("#query-desc").html(eq[3]);
        //jQuery("#query-desc").html("Find all architects who studies in University of Oxford.");
        document.getElementById("entry_31427235").value = "1";
        break;
}


jQuery("#useful-tips-hide").click(function() {
    if (usefulTipsFlag == 1) {
        usefulTipsFlag = 0;
        jQuery("#useful-tips-list").hide();
    } else {
        usefulTipsFlag = 1;
        jQuery("#useful-tips-list").show();
    }
});


jQuery('#suggestion-options').change(function() {
    noOfSuggestions = jQuery("#suggestion-options option:selected").val();
    if(nodes.length > 0) {
        refreshAllSuggestion();
    }
});

jQuery("#node-edit-selectDiv").click(function() {
    jQuery("#node-edit-entity-options").attr('size', 19);
});


jQuery("#Add-Edge-button").click(function() {
    if (addEdgeButtonEdgeAdded == false) {
        addEdgeButtonEdgeAdded = true;
        var newNode = {
            id: ++lastNodeId,
            name: exampleTarget.name,
            flag: 2,
            greyflag: 1,
            nodeID: exampleTarget.nodeID,
            tempName: exampleTarget.name
        };
        var link = {
            source: exampleSource,
            target: newNode,
            value: exampleLinkValue,
            id: ++lastEdgeID,
            flag: 2,
            linkID: exampleLinkId,
            linkNum: 1,
            actualSourceType: exampleActualSourceType,
            actualTargetType: exampleActualTargetType
        };
        nodes.push(newNode);
        links.push(link);
        var newNode = {
            id: ++lastNodeId,
            name: exampleSource.name,
            flag: 2,
            greyflag: 1,
            nodeID: exampleSource.nodeID,
            tempName: exampleSource.name
        };
        var link = {
            source: newNode,
            target: exampleTarget,
            value: exampleLinkValue,
            id: ++lastEdgeID,
            flag: 2,
            linkID: exampleLinkId,
            linkNum: 1,
            actualSourceType: exampleActualSourceType,
            actualTargetType: exampleActualTargetType
        };
        nodes.push(newNode);
        links.push(link);
        suggestions = true;
        suggestionPicked = false;
        //         jQuery("#button-g").hide();
        animatedHelp();
        restart();
    }
});



jQuery("#Reverse-button").click(function() {
    var tempObject;
    var tempSource;
    var tempExampleSource;
    var tempExampleObject;
    tempObject = response[0].objectType;
    tempSource = response[0].sourceType;
    response[0].objectType = tempObject;
    response[0].sourceType = tempSource;
    for (var k = 0; k < response[0].examples.length; k++) {
        tempExampleSource = response[0].examples[k].split(',')[0];
        tempExampleObject = response[0].examples[k].split(',')[1];
        response[0].examples[k] = tempExampleObject + ',' + tempExampleSource;
    }
    animatedExamples();
    var linkNum = 1;
    for (var i = 0; i < links.length; i++) {
        if (links[i].source.id == exampleTarget.id && links[i].target.id == exampleSource.id && links[i].source != links[i].target) {
            if (linkNum > links[i].linkNum) {} else if (linkNum == links[i].linkNum) {
                linkNum++;
            } else {
                linkNum = links[i].linkNum++;
            }
        }
    }
    for (var i = 0; i < links.length; i++) {
        if (links[i].source === exampleSource && links[i].target === exampleTarget && links[i].linkID == exampleLinkId) {
            var tempTarget = exampleTarget;
            var tempSource = exampleSource;
            links[i].source = tempTarget;
            links[i].target = tempSource;
            exampleTarget = tempSource;
            exampleSource = tempTarget;
            links[i].linkNum = linkNum;
        }

    }
    restart();
});




/*
THe method gets invoked whenever the 'Help' button is clicked on the suggestions pane.
It displays suggestions based on the context.
*/
jQuery("#show-instructions").click(function() {
    if (showInstructionsFlag == 0) {
        showInstructionsFlag = 1;
        if (newNode && newEdge == false) {
            nodeNameText(selected_node, 1);
        } else if (newNode == false && newEdge == false) {
            nodeNameText(selected_node, 2);
        } else {
            nodeNameText(selected_node, 3);
        }
    } else {
        jQuery("#modal-text").empty();
        showInstructionsFlag = 0;
    }
});

//Adds the svg to the graph.
var svg = d3.select('#svg-div')
    .append('svg')
    .attr('width', jQuery('#svg-div').width())
    .attr('height', "650") //changed for FA lab monitors
    .attr('id', 'queryConstructSVG');
// .call(zoom);

//Sets the x location of the 'Try Different Suggestions' button.
// var x = jQuery('#svg-div').width()-240;

//Creates the 'Try Different Suggestions' button.
// var buttonG = d3.select('#button-g') //Edited by AD
// //                   .attr("transform", "translate("+x+",0)")
//                   // .attr("x", 1000)
//                   // .attr("y", 17)
//                   .attr("width", 230)
//                   .attr("height", 47.5)
//                   // .attr('fill', "blue")
//                   .attr("id", "button-g")
//                   .on('mouseover', function(){
//                      this.style.cursor='pointer';
//                      nextButtonClick = true;
//                   })
//                   .on('mouseout', function(){
//                     nextButtonClick = false;
//                   })

//                   buttonG.on("click", function(){
//                     nextButtonClick = true;
//                     console.log("g clicked");
//                   });
// jQuery("#button-g").on('mouseover', function(){
// //                      this.style.cursor='pointer';
//                      nextButtonClick = true;
//                   });
//                   jQuery("#button-g").on('mouseout', function(){
//                     nextButtonClick = false;
//                   });
// .on('click', function(){
//   nextButtonClick = true;
//   removeTempNodes();
//   nextButtonClick = false;
//   AddSuggestions();
// });

// var refresh = buttonG.append('text')
//                  .text('Try Different Suggestions').attr("y", 100).attr("x",28)
//                   .attr("font-family", "Segoe UI")
//                   .attr("font-size", "18px")
//                   .attr("fill", "white")
//                   .style("font-weight","500")
//                   .on('click', function(){
//                     nextButtonClick = true;
//                   });

//Edited by AD
jQuery("#button-g").click(function() {

    refreshAllSuggestion();

});


function refreshAllSuggestion() {
    nextButtonClick = true;
    removeTempNodes();
    nextButtonClick = false;

    var singleNode = 0;
    if (nodes.length == 1) {
        singleNode = 1;
    }
    //   var suggestion_mode = 1;
    //   if (selected_node!=null){
    //     suggestion_mode = 2;
    //   }
    restart();
    refreshSelectedNode = 0; //no node is selected thus making it zero
    // if(suggestion_mode!=0){
    //  addSuggestions(singleNode, 2);
    // }
    //   else{
    addSuggestions(singleNode, 1);
    //   }


    //     nextButtonClick = false;
}
// jQuery("#button-g").hide();

var g = svg.append('svg:g').attr('id', 'queryConstructG');
var nodes = [],
    lastNodeId = 2,
    links = [],
    lastEdgeID = 2;
for (var i = 0; i < links.length; i++) {
    value[i] = links[i].value;
};
var currentEdges = links.length;
// init D3 force layout
var force = d3.layout.force()
    .nodes(nodes)
    .links(links)
    .size([width, height])
    .linkDistance(300)
    .charge(-1000)
    .on('tick', tick);

var justDragged = false;
//Setting up drag behavior on nodes. Calls respective functions on dragstart, drag and dragend.
var drag = d3.behavior.drag()
    .on("dragstart", dragstart)
    .on("drag", dragmove)
    .on("dragend", dragend);

var defs = g.append("svg:defs").selectAll("marker");
defs.attr("id", "defs-marker");

// line displayed when dragging new nodes
var drag_line = g.append('svg:path')
    .attr('class', 'link dragline hidden')
    // .attr('d', 'M0,0L0,0');
    .attr('d', 'M0,-5L10,0L0,5');

// handles to link and node element groups
var path = g.append('svg:g').attr("id", "pathG").selectAll('path'),
    circle = g.append('svg:g').attr('id', 'circleG').selectAll('g');

var linkText = g.append('svg:g').attr('id', "linktextg")
    .selectAll('g.linkLabelHolder');
// var tooltip = d3.select("body")
//     .append("div")
//     .style("position", "absolute")
//     .style("z-index", "10")
//     .style("visibility", "hidden")
//     .style("background-color", "#E6EBFA")
//     .style("border-radius" , "8px")
//     .style("font-weight" , "bold")
//     .style("font-family" , "Segoe UI")
//     .style("opacity", "0.9")
//     .style("border-width", "3px")
//     .style("padding", "8px")
//     .style("border-color", "#B2C2F0")
//     .style("border-style", "ridge");
//     .text("a simple tooltip");

var tooltipEdgeExample = d3.select("body")
    .append("div").attr("id", "button-container")
    .style("position", "absolute")
    .style("z-index", "10")
    .style("visibility", "hidden")
    .style("background-color", "#E6EBFA")
    .style("border-radius", "8px")
    .style("font-weight", "bold")
    .style("font-family", "Segoe UI")
    .style("opacity", "0.9")
    .style("border-width", "3px")
    .style("text-align", "left")
    .style("padding-top", "8px") //Edited by AD
    .style("padding-right", "30px") //Edited by AD
    .style("border-color", "#B2C2F0")
    .style("border-style", "ridge");
jQuery("#button-container").append('<div id="forTextColorChange" style="text-align:center;float:left; margin-left: 10px;height:100%; margin-right: 10px; border-style: none dashed none none;"><button id="linkDel" style="text-align:center; border-style: outset; margin-right:10px;margin-top:45px ;margin-bottom:40px ;vertical-align:middle;"></button></div>');
// jQuery("#tooltip-examples").append('<div style="text-align:center;float:left;"><button id="linkDel" style="text-align:center; border: solid black;"></button></div>');
// tooltipEdgeExample.append("div").style("float","right")
tooltipEdgeExample.append("ul").attr("id", "tooltip-examples").style("left", "0%")
    .on("mouseover", function() {
        mouseOnOption = true;
        alpha = force.alpha();
        force.stop();
        tooltipEdgeExample.style("visibility", "visible");

        //   if(selected_link.source.greyflag==0)
        if (selected_link == null) {
            return;
        }
        jQuery("#node" + selected_link.source.id).css("fill", "#0047ab");
        //              if(selected_link.target.greyflag==0){
        jQuery("#node" + selected_link.target.id).css("fill", "#00CC00");
        //             selected_link = null;
    })
    .on("mouseout", function() {
        mouseOnOption = false;
        tooltipEdgeExample.style("visibility", "hidden");
        //   force.start();
        force.alpha(alpha);
        if (selected_link == null) {
            return;
        }
        jQuery("#node" + selected_link.target.id).css("fill", "#000000");
        jQuery("#node" + selected_link.source.id).css("fill", "#000000");
        //   selected_link = null;
    });
// jQuery("#tooltip-examples").on("mouseover",function(){
//   jQuery("#tooltip-examples").show();
// })
// .on("mouseout",function(){
//   jQuery("#tooltip-examples").hide();
// });
// tooltipEdgeExample.append("div").attr("id","delAddButton").style("text-align","center");
jQuery("#forTextColorChange").on("mouseover", function() {
        alpha = force.alpha();
        force.stop();
        // if(selected_link.source.greyflag==0)
        if (selected_link == null) {
            return;
        }
        jQuery("#node" + selected_link.source.id).css("fill", "#0047ab");
        // if(selected_link.target.greyflag==0){
        jQuery("#node" + selected_link.target.id).css("fill", "#00CC00");
    })
    .on("mouseout", function() {
        //   force.start();
        force.alpha(alpha);
        if (selected_link == null) {
            return;
        }
        jQuery("#node" + selected_link.source.id).css("fill", "#000000");
        jQuery("#node" + selected_link.target.id).css("fill", "#000000");
    });

//<!--Edited by AD
var delNodeLabel = d3.select("body")
    .append("div")
    .style("position", "absolute")
    .style("z-index", "100")
    .style("cursor", "pointer")
    .style("border", "1px solid black")
    .style("background-color", "white")
    .style("padding-right", "3px")
    .style("padding-left", "3px")
    .style("visibility", "hidden")


var delNode = d3.select("body")
    .append("div")
    .style("position", "absolute")
    .style("z-index", "10")
    .style("cursor", "pointer")

    .style("visibility", "hidden")
    .style("background-color", "#E6EBFA")
    .style("border-radius", "20px")
    .style("height", "40px")
    .style("width", "40px")
    .style("border-width", "2px")
    .style("border-color", "black")
    //     .style("border-radius" , "8px")
    //     .style("font-weight" , "bold")
    //     .style("font-family" , "Segoe UI")
    //     .style("opacity", "0.9")
    //     .style("border-width", "3px")
    .style("text-align", "left")
    .style("padding", "8px") //Edited by AD
    //     .style("padding-right", "30px")//Edited by AD
    //     .style("border-color", "#B2C2F0")
    //     .style("border-style", "ridge")
    .style("-webkit-transition", "all 0.4s ")
    .style("transition", "all 0.4s ")
    //   .style("-webkit-transition-delay"," 0.1s")
    //   .style("transition-delay", "0.1s")

    .attr("id", "node-delete")
    .on("mouseover", function() {
        mouseOnOption = true;
        alpha = force.alpha();
        force.stop();
        delNode.style("visibility", 'visible');
        delNodeLabel.style("visibility", "visible").style("top", (event.pageY) + "px").style("left", (event.pageX + 20) + "px")
            .text(function() {
                if (selected_node.greyflag == 0) {
                    return "Delete"
                } else {
                    return "Add"
                }
            });
    })
    .on("mouseout", function() {
        mouseOnOption = false;
        //   force.start();
        force.alpha(alpha);
        delNode.style("visibility", 'hidden');
        delNodeLabel.style("visibility", 'hidden');
    });
delNode.append("i")

    .attr("class", "material-icons")
    .style("border-color", "black")
    .attr("id", "delAdd")
    // .text("Delete")
    // .style("left","0%")
    .on("mouseover", function() {
        alpha = force.alpha();
        force.stop();
        delNode.style("visibility", 'visible');
        delNodeLabel.style("visibility", "visible").style("top", (event.pageY) + "px").style("left", (event.pageX) + "px")
            .text(function() {
                if (selected_node.greyflag == 0) {
                    return "Delete"
                } else {
                    return "Add"
                }
            });
    })
    .on("mouseout", function() {
        //   force.start();
        force.alpha(alpha);
        delNode.style("visibility", 'hidden');
        delNodeLabel.style("visibility", 'hidden');
    })
    .on("click", function() {
        if (selected_node) {
            updateUndo();
            if (selected_node.greyflag != 1) {
                for (var x = 0; x < links.length; x++) {
                    if (links[x].source === selected_node && (links[x].target.greyflag == 1 || links[x].target.greyflag == 2)) {
                        nodes.splice(nodes.indexOf(links[x].target), 1);
                    } else if (links[x].target === selected_node && (links[x].source.greyflag == 1 || links[x].source.greyflag == 2)) {
                        nodes.splice(nodes.indexOf(links[x].source), 1);
                    }
                }
                nodes.splice(nodes.indexOf(selected_node), 1);
                spliceLinksForNode(selected_node);
                var graphDisconnect = checkConnected(nodes, links);
                if (graphDisconnect == true) {
                    //             jQuery("#button-g").hide();
                } else {
                    jQuery("#submit-button").attr("class", "menu-border");
                    jQuery("#button-g").attr("class", "menu-border");
                }
                //         path.exit().remove();




                var graphDisconnect = checkConnected(nodes, links);
                if (graphDisconnect == true) {
                    //             jQuery("#button-g").hide();
                } else {

                    jQuery("#submit-button").attr("class", "menu-border");
                    jQuery("#button-g").attr("class", "menu-border");
                }
                selected_link = null;
                selected_node = null;
                animatedHelp();
                delNode.style("visibility", "hidden");

                restart();
                if (nodes.length == 0) {
                    jQuery("#button-g").attr("class", "inactive");
                    jQuery("#submit-button").attr("class", "inactive");

                } else {
                    jQuery("#button-g").attr("class", "menu-border");
                    jQuery("#submit-button").attr("class", "menu-border");
                }
            } else {
                // if (drawEdge == true)
                //          {
                // nodeclick = false;

                allowNodeCreation = true;
                //             if (mousedown_node.greyflag == 1 && mousedown_node.flag == 2 && selected_node == null){
                selected_node.greyflag = 2;
                selected_node.flag = 1;
                noOfSuggestionsPicked++;
                if (noOfSuggestionsPicked == 0 || noOfSuggestionsPicked == 1) {
                    displayFlag = 1;
                }
                if (subSuggestionsInProgressFlag == true) {
                    SugPickdPerGreyLinkCount++;
                    if (SugPickdPerGreyLinkCount == 1 && greyLinkPickdPerSubSuggestion == 0 && noOfSuggestionsPicked > 1) {
                        displayFlag = 1;
                    }
                    // animatedHelp();
                }
                //            suggestionPicked = true;
                //            jQuery("#button-g").hide();
                // }
                delNode.style("visibility", "hidden");
                suggestionsFinalized = true;
                //restart();
            }


            removeTempNodes();
            var singleNode = 0;
            if (nodes.length == 1) {
                singleNode = 1;
            }
            restart();
            refreshSelectedNode = 0;
            addSuggestions(singleNode, 1);
        }
        //       else if(selected_link) {
        //         jQuery("#edge"+selected_link.id.toString()).remove();
        //         jQuery("#linkId_"+selected_link.id.toString()).remove();
        //         jQuery("#linkLabelHolderId_"+selected_link.id.toString()).remove();
        //         selected_link.source = selected_link.target;
        //         if (edgeTypesFlag == 1){
        //           jQuery("#wrap4").toggleClass('active');
        //           jQuery("#edge-types").toggleClass('active');
        //           edgeTypesFlag = 0;
        //         }
        //         if (exampleReverseRoleFlag == true){
        //            exampleSource = null;
        //            exampleTarget = null;
        //            exampleLinkId = -1;
        //            exampleLinkGraphId = -1;
        //            exampleReverseRoleFlag = false;
        //            exampleActualSourceType = null;
        //            exampleActualTargetType = null;
        //            addEdgeButtonEdgeAdded = false;
        //            jQuery("#Add-Edge-button").hide();
        //            jQuery("#example-text").show();
        //            jQuery("#active-examples").empty();
        //            jQuery("#Reverse-button").hide();
        //         }
        //         // links.splice(links.indexOf(selected_link), 1);
        //         var graphDisconnect = checkConnected(nodes, links);
        //         if (graphDisconnect == true){
        //             jQuery("#button-g").hide();
        //         }else{
        //            jQuery("#button-g").show();
        //         }
        //       }
        //       selected_link = null;
        //       selected_node = null;
        //       animatedHelp();
        //       delNode.style("visibility", "hidden");
        //       restart();
    }); //-->
var editNodeLabel = d3.select("body")
    .append("div")
    .style("position", "absolute")
    .style("z-index", "100")
    .style("cursor", "pointer")
    .style("border", "1px solid black")
    .style("background-color", "white")
    .style("padding-right", "3px")
    .style("padding-left", "3px")
    .style("visibility", "hidden")
    .text("Edit")

var editNode = d3.select("body")
    .append("div")
    .style("position", "absolute")
    .style("z-index", "10")
    .style("cursor", "pointer")

    .style("visibility", "hidden")
    .style("background-color", "#E6EBFA")
    .style("border-radius", "20px")
    .style("height", "40px")
    .style("width", "40px")
    //     .style("font-weight" , "bold")
    //     .style("font-family" , "Segoe UI")
    //     .style("opacity", "")
    .style("border-width", "2px")
    .style("border-color", "black")
    .style("text-align", "left")
    // .style("height","40px")
    // .style("width","40px")
    .style("-webkit-transition", "all 0.6s cubic-bezier(.25,.8,.25,1)")
    .style("transition", "all 0.6s cubic-bezier(.25,.8,.25,1)")
    //   .style("-webkit-transition-delay"," 0.2s")
    //   .style("transition-delay", "0.2s")
    .style("padding", "8px") //Edited by AD
    //     .style("padding-right", "30px")//Edited by AD
    //     .style("border-color", "#B2C2F0")
    //     .style("border-style", "ridge")
    .attr("id", "node-edit")
    // .attr("title","Edit")
    .on("mouseover", function() {
        editNode.style("visibility", 'visible');

        editNodeLabel.text("Set a type/entity");

        editNodeLabel.style("visibility", "visible").style("top", (event.pageY) + "px").style("left", (event.pageX + 20) + "px");
    })
    .on("mouseout", function() {
        editNode.style("visibility", 'hidden');
        editNodeLabel.style("visibility", 'hidden');
    })

editNode.append("i")
    .attr("class", "material-icons")
    .style("border-color", "black")
    .text("create")


    .on("mouseover", function() {
        alpha = force.alpha();
        force.stop();
        mouseOnOption = true;
        editNode.style("visibility", 'visible');
        editNodeLabel.style("visibility", "visible").style("top", (event.pageY) + "px").style("left", (event.pageX + 20) + "px");
    })
    .on("mouseout", function() {
        mouseOnOption = false;
        //   force.start();
        force.alpha(alpha);
        editNode.style("visibility", 'hidden');
        editNodeLabel.style("visibility", 'hidden');
    })
    .on("click", function() {

        clearIndexGlobalVariables();
        nodeToBeEdited = selected_node;
        curNodeLabel = null;
        jQuery("#type-selected-value").attr('value', -1);
        jQuery("#type-selected").attr('value', "Select Type...");
        jQuery("#entity-selected-value").attr('value', -1);
        jQuery("#entity-selected").attr('value', "Select Entity...");
        jQuery("#domain-selected-value").attr('value', -1);
        jQuery("#domain-selected").attr('value', "Select Domain...");
        jQuery("#type-selected").hide();
        jQuery("#type-x-button").hide();
        jQuery("#entity-x-button").hide();
        jQuery("#entity-selected").hide();
        jQuery("#domain-x-button").hide();
        jQuery("#domain-selected").hide();

        setToFirstPage();

        jQuery("#modal-title").empty();
        jQuery("#modal-title").append("<h4>Set a type/entity</h4>");
        jQuery("#domain-options").prop('disabled', false);
        jQuery("#type-options").prop('disabled', false);
        jQuery("#entity-options").prop('disabled', false);
        jQuery("#modal-text").empty();
        jQuery('#myModal').modal('show');
        jQuery("#add-type-text").hide();
        jQuery("#add-type-button").hide();
        jQuery("#type-list").html(tdListText).show();
        jQuery("#dtl tr:gt(0)").remove();
        jQuery("#dtl").show();

        curEndTypeList = findEdgeEndTypes(nodeToBeEdited);

        curTypeList = []
        if(nodeToBeEdited.typeList != undefined) {
          curTypeList = nodeToBeEdited.typeList.slice();
        }

        clearAllSelection();
        updateDomainsTypesEntities("","");

        domains = generatedDomains.slice();
        types = generatedTypes.slice();
        entities = generatedEntities.slice();
        displayDomainOptions(domains);
        displayTypeOptions(types);
        displayEntityOptions(entities);

        var typeDomainLabels = curTypeList.slice();
        for(var elem of curEndTypeList) {
          if(typeDomainLabels.includes(elem) == false) {
            typeDomainLabels.push(elem);
          }
        }

        jQuery("#type-list").html(tdListText);
        showDtlTable("dtl", typeDomainLabels);

        prevDomSel = null;
        prevTypSel = null;
        if (nodeToBeEdited.entity == -1) {
          prevEntSel = null;
        } else {
          prevEntSel = nodeToBeEdited.nodeID;
          jQuery("#entity-selected").attr("value", nodeToBeEdited.name);
          // jQuery("#edit-entity-selected").show();
          // jQuery("#edit-entity-x-button").show();
          setTimeout(function() {
              var ot = jQuery('#entity-options').find('[value="' + nodeToBeEdited.nodeID + '"]').offset().top;
              var st = jQuery('#entity-options').offset().top;
              jQuery('#entity-options').scrollTop(jQuery('#entity-options').scrollTop() + ot - st);
              jQuery('#entity-options').find('[value="' + nodeToBeEdited.nodeID + '"]').attr('selected', true);

          }, 700);
        }


        // if(true) {
        // // if (dblClickValid == true) {
        //     //              nodeToBeEdited = selected_node;
        //     editNodeKeyword = "";
        //     nodeEditList.length = 0;
        //     nodeEditEdges.length = 0;
        //     nodeEditWindowNo = 0;
        //     nodeEditEntityWindoNo = 0;
        //     var count = 0;
        //     for (var k = 0; k < links.length; k++) {
        //         if (links[k].source != links[k].target && links[k].source === nodeToBeEdited) {
        //             if (count == 0) {
        //                 count++;
        //                 nodeEditEdges = links[k].linkID + "," + "0";
        //             } else {
        //                 nodeEditEdges = nodeEditEdges + "|" + links[k].linkID + "," + "0";
        //             }
        //         } else if (links[k].source != links[k].target && links[k].target === nodeToBeEdited) {
        //             if (count == 0) {
        //                 count++;
        //                 nodeEditEdges = links[k].linkID + "," + "1";
        //             } else {
        //                 nodeEditEdges = nodeEditEdges + "|" + links[k].linkID + "," + "1";
        //             }
        //         }
        //
        //     }
        //     jQuery("#edit-entity-selected").hide();
        //     jQuery("#edit-entity-x-button").hide();
        //     jQuery("#edit-type-selected").hide();
        //     jQuery("#edit-type-x-button").hide();
        //     jQuery("#edit-domain-selected").hide();
        //     jQuery("#edit-domain-x-button").hide();
        //     jQuery("#edit-type-selected-value").attr('value', -1);
        //     jQuery("#edit-entity-selected-value").attr('value', -1);
        //     jQuery("#edit-domain-selected-value").attr('value', -1);
        //     prevDomSel = null;
        //     prevTypSel = null;
        //
        //     nodeEditWindowNo = 0;
        //     nodeEditTypeWindowNo = 0;
        //     if (nodeToBeEdited.entity == -1) {
        //         prevEntSel = null;
        //         var typeListNode = loadEditTypeList();
        //         loadEditDomainList(typeListNode);
        //         loadEditEntityList(-1, nodeToBeEdited.nodeID);
        //         jQuery("#edit-type-selected-value").attr('value', nodeToBeEdited.nodeID);
        //         jQuery("#node-edit-entity-options").show();
        //         jQuery("#node-edit-entity-search").show();
        //         jQuery("#node-edit-save-changes").css("background-color", "#828b8f");
        //         jQuery("#nodeEditModal").modal('show');
        //         jQuery("#edit-add-type-text").hide();
        //         jQuery("#edit-add-type-button").hide();
        //
        //         curTypeList = []
        //         if(nodeToBeEdited.typeList != undefined) {
        //           curTypeList = nodeToBeEdited.typeList.slice();
        //         }
        //
        //         var typeDomainLabels = curTypeList.slice();
        //         for(var elem of curEndTypeList) {
        //           if(typeDomainLabels.includes(elem) == false) {
        //             typeDomainLabels.push(elem);
        //           }
        //         }
        //         // if(Array.isArray(curTypeList) && curTypeList.length) {
        //         //     typeDomainList += curTypeList.join(', ');
        //         // }
        //         // if(Array.isArray(curEndTypeList) && curEndTypeList.length) {
        //         //     if(typeDomainList != "") {
        //         //       typeDomainList += ", ";
        //         //     }
        //         //     typeDomainList += curEndTypeList.join(', ');
        //         // }
        //         // var typeIds = [];
        //         // var typeDomainLabels = [];
        //         //
        //         // for(var elem of typeDomainList.split(', ')) {
        //         //   if(typeDomainList == "") break;
        //         //   if(typeIds.includes(elem.split(':')[0]) == false) {
        //         //     typeIds.push(elem.split(':')[0]);
        //         //     typeDomainLabels.push(elem.split(':').slice(1,3).join(':'));
        //         //   }
        //         // }
        //
        //         //jQuery("#edit-type-list").html(tdListText+typeDomainLabels.join(', '));
        //         jQuery("#edit-type-list").html(tdListText);
        //
        //         showDtlTable("edit-dtl", typeDomainLabels);
        //
        //         jQuery("#node-edit-border").show();
        //         // jQuery("#edit-type-selected").attr("value",nodeToBeEdited.tempName);
        //         // jQuery("#edit-type-selected").show();
        //         // jQuery("#edit-type-x-button").show();
        //         // setTimeout(function(){
        //         //   var ot = jQuery('#node-edit-type-options').find('[value="'+nodeToBeEdited.nodeID+'"]').offset().top;
        //         //   var st = jQuery('#node-edit-type-options').offset().top;
        //         //   jQuery('#node-edit-type-options').scrollTop(jQuery('#node-edit-type-options').scrollTop()+ot-st);
        //         //   jQuery('#node-edit-type-options').find('[value="'+nodeToBeEdited.nodeID+'"]').attr('selected', true);
        //         // },500);
        //     } else {
        //         prevEntSel = nodeToBeEdited.nodeID;
        //         var typeListNode = loadEditTypeList();
        //         loadEditDomainList(typeListNode);
        //
        //         nodeEditWindowNo = -1;
        //         jQuery("#node-edit-entity-options").show();
        //         jQuery("#node-edit-entity-search").show();
        //         loadEditEntityList();
        //         jQuery("#edit-entity-selected-value").attr('value', nodeToBeEdited.nodeID);
        //         jQuery("#node-edit-save-changes").css("background-color", "#828b8f");
        //         jQuery("#nodeEditModal").modal('show');
        //         jQuery("#edit-add-type-text").hide();
        //         jQuery("#edit-add-type-button").hide();
        //
        //         curTypeList = nodeToBeEdited.typeList.slice();
        //         var typeDomainLabels = curTypeList.slice();
        //         for(var elem of curEndTypeList) {
        //           if(typeDomainLabels.includes(elem) == false) {
        //             typeDomainLabels.push(elem);
        //           }
        //         }
        //         // var typeDomainList = "";
        //         // if(Array.isArray(nodeToBeEdited.typeList) && nodeToBeEdited.typeList.length) {
        //         //     typeDomainList += nodeToBeEdited.typeList.join(', ');
        //         // }
        //         // if(Array.isArray(curEndTypeList) && curEndTypeList.length) {
        //         //     if(typeDomainList != "") {
        //         //       typeDomainList += ", ";
        //         //     }
        //         //     typeDomainList += curEndTypeList.join(', ');
        //         // }
        //         // var typeIds = [];
        //         // var typeDomainLabels = [];
        //         // for(var elem of typeDomainList.split(', ')) {
        //         //   if(typeIds.includes(elem.split(':')[0]) == false) {
        //         //     typeIds.push(elem.split(':')[0]);
        //         //     typeDomainLabels.push(elem.split(':').slice(1,3).join(':'));
        //         //   }
        //         // }
        //
        //         jQuery("#edit-type-list").html(tdListText);
        //
        //         showDtlTable("edit-dtl", typeDomainLabels);
        //
        //         jQuery("#node-edit-border").show();
        //         jQuery("#edit-entity-selected").attr("value", nodeToBeEdited.name);
        //         // jQuery("#edit-entity-selected").show();
        //         // jQuery("#edit-entity-x-button").show();
        //         setTimeout(function() {
        //             var ot = jQuery('#node-edit-entity-options').find('[value="' + nodeToBeEdited.nodeID + '"]').offset().top;
        //             var st = jQuery('#node-edit-entity-options').offset().top;
        //             jQuery('#node-edit-entity-options').scrollTop(jQuery('#node-edit-type-options').scrollTop() + ot - st);
        //             jQuery('#node-edit-entity-options').find('[value="' + nodeToBeEdited.nodeID + '"]').attr('selected', true);
        //
        //         }, 700);
        //     }
        //
        // }
    });
var suggNodeLabel = d3.select("body")
    .append("div")
    .style("position", "absolute")
    .style("z-index", "100")
    .style("cursor", "pointer")
    .style("border", "1px solid black")
    .style("background-color", "white")
    .style("padding-right", "3px")
    .style("padding-left", "3px")
    .style("visibility", "hidden")
    .text("Suggest for this node only")

var suggNode = d3.select("body")
    .append("div")
    .style("position", "absolute")
    .style("z-index", "10")
    .style("cursor", "pointer")

    .style("visibility", "hidden")
    .style("background-color", "#E6EBFA")
    .style("border-radius", "20px")
    .style("height", "40px")
    .style("width", "40px")
    //     .style("font-weight" , "bold")
    //     .style("font-family" , "Segoe UI")
    //     .style("opacity", "")
    .style("border-width", "2px")
    .style("border-color", "black")
    .style("text-align", "left")
    // .style("height","40px")
    // .style("width","40px")
    .style("-webkit-transition", "all 0.2s cubic-bezier(.25,.8,.25,1)")
    .style("transition", "all 0.2s cubic-bezier(.25,.8,.25,1)")
    //   .style("-webkit-transition-delay"," 0s")
    //   .style("transition-delay", "0s")
    .style("padding", "8px") //Edited by AD
    //     .style("padding-right", "30px")//Edited by AD
    //     .style("border-color", "#B2C2F0")
    //     .style("border-style", "ridge")
    .attr("id", "node-edit")
    .on("mouseover", function() {
        alpha = force.alpha();
        force.stop();
        mouseOnOption = true;
        if(systemName === "naive") suggNode.style("visibility", 'hidden');
        else suggNode.style("visibility", 'visible');
        suggNodeLabel.style("visibility", "visible").style("top", (event.pageY) + "px").style("left", (event.pageX + 20) + "px");
    })
    .on("mouseout", function() {
        mouseOnOption = false;
        //   force.start();
        force.alpha(alpha);
        suggNode.style("visibility", 'hidden');
        suggNodeLabel.style("visibility", 'hidden');
    });
suggNode.append("i")
    .attr("class", "material-icons")
    .style("border-color", "black")
    .text("lightbulb")
    // .attr("title","Suggest for this node only")

    .on("mouseover", function() {
        alpha = force.alpha();
        force.stop();
        if(systemName === "naive") suggNode.style("visibility", 'hidden');
        else suggNode.style("visibility", 'visible');
        suggNodeLabel.style("visibility", "visible").style("top", (event.pageY) + "px").style("left", (event.pageX + 20) + "px");
    })
    .on("mouseout", function() {
        //   force.start();
        force.alpha(alpha);
        suggNode.style("visibility", 'hidden');
        suggNodeLabel.style("visibility", 'hidden');
    })
    .on("click", function() {
        nextButtonClick = true;
        removeTempNodes();
        nextButtonClick = false;

        var singleNode = 0;
        if (nodes.length == 1) {
            singleNode = 1;
        }
        restart();
        refreshSelectedNode = suggestion_mode;

        addSuggestions(singleNode, 1);
    });

// mouse event vars
var selected_node = null,
    selected_link = null,
    mousedown_link = null,
    mousedown_node = null,
    mouseup_node = null,
    allowNodeDrag = false,
    link_click = false;

jQuery('#undo-button').click(function() {
    if (jQuery("#undo-text").text() == "Undo") {
        updateRedo();
        jQuery('defs').empty();
        jQuery('#pathG').empty();
        jQuery('#circleG').empty();
        jQuery('#linktextg').empty();

        nodes.length = 0;

        var nDict = {};
        for (var i = 0; i < undo_nodes.length; i++) {
            nDict[undo_nodes[i].id] = ++lastNodeId;
            var n = {
                id: lastNodeId,
                name: undo_nodes[i].name,
                flag: undo_nodes[i].flag,
                greyflag: undo_nodes[i].greyflag,
                nodeID: undo_nodes[i].nodeID,
                domain: undo_nodes[i].domain,
                type: undo_nodes[i].type,
                entity: undo_nodes[i].entity,
                tempName: undo_nodes[i].tempName,
                x: undo_nodes[i].x,
                y: undo_nodes[i].y,
                px: undo_nodes[i].px,
                py: undo_nodes[i].py,
                index: undo_nodes[i].index,
                weight: undo_nodes[i].weight
            }
            nodes.push(n);

        }

        links.length = 0;
        restart();
        for (var i = 0; i < links.length; i++) {
            links[i].source = links[i].target;
        }
        for (var i = 0; i < undo_links.length; i++) {

            var s = undo_links[i].source,
                t = undo_links[i].target;
            for (var j = 0; j < nodes.length; j++) {
                if (nDict[undo_links[i].source.id] == nodes[j].id) {
                    s = nodes[j];
                }
                if (nDict[undo_links[i].target.id] == nodes[j].id) {
                    t = nodes[j];
                }
            }

            links.push({
                source: s,
                target: t,
                value: undo_links[i].value,
                id: ++lastEdgeID,
                flag: undo_links[i].flag,
                linkID: undo_links[i].linkID,
                linkNum: undo_links[i].linkNum,
                actualSourceType: undo_links[i].actualSourceType,
                actualTargetType: undo_links[i].actualTargetType
            });
            restart();
        }
        jQuery("#undo-text").text("Redo");
        jQuery("#undo-icon").text("redo");
        restart();

    } else {
        jQuery('defs').empty();
        jQuery('#pathG').empty();
        jQuery('#circleG').empty();
        jQuery('#linktextg').empty();

        nodes.length = 0;

        var nDict = {};
        for (var i = 0; i < redo_nodes.length; i++) {
            nDict[redo_nodes[i].id] = ++lastNodeId;
            var n = {
                id: lastNodeId,
                name: redo_nodes[i].name,
                flag: redo_nodes[i].flag,
                greyflag: redo_nodes[i].greyflag,
                nodeID: redo_nodes[i].nodeID,
                domain: redo_nodes[i].domain,
                type: redo_nodes[i].type,
                entity: redo_nodes[i].entity,
                tempName: redo_nodes[i].tempName,
                x: redo_nodes[i].x,
                y: redo_nodes[i].y,
                px: redo_nodes[i].px,
                py: redo_nodes[i].py,
                index: redo_nodes[i].index,
                weight: redo_nodes[i].weight
            }
            nodes.push(n);

        }

        links.length = 0;
        restart();
        for (var i = 0; i < links.length; i++) {
            links[i].source = links[i].target;
        }
        for (var i = 0; i < redo_links.length; i++) {

            var s = redo_links[i].source,
                t = redo_links[i].target;
            for (var j = 0; j < nodes.length; j++) {
                if (nDict[redo_links[i].source.id] == nodes[j].id) {
                    s = nodes[j];
                }
                if (nDict[redo_links[i].target.id] == nodes[j].id) {
                    t = nodes[j];
                }
            }

            links.push({
                source: s,
                target: t,
                value: redo_links[i].value,
                id: ++lastEdgeID,
                flag: redo_links[i].flag,
                linkID: redo_links[i].linkID,
                linkNum: redo_links[i].linkNum,
                actualSourceType: redo_links[i].actualSourceType,
                actualTargetType: redo_links[i].actualTargetType
            });
            restart();
        }
        jQuery("#undo-text").text("Undo");
        jQuery("#undo-icon").text("undo");
        restart();

    }
    refreshAllSuggestion();
    updateQueryResultTable(true);
});

function showDtlTable(tableId, typeDomainLabels) {
  var dtl = document.getElementById(tableId);
  jQuery("#"+tableId+" tr:gt(0)").remove();
  for(var elem of typeDomainLabels) {
    var row = dtl.insertRow(-1);
    var cell1 = row.insertCell(0);
    var cell2 = row.insertCell(1);
    var cell3 = row.insertCell(2);
    var domain = elem.split(":")[1].toUpperCase();
    var type = elem.split(":")[2].toUpperCase();
    cell1.innerHTML = "<i>"+domain+"</i>";
    cell2.innerHTML = type;
    if(curEndTypeList.contains(elem) == false) {
      cell3.innerHTML = "<button class='editbtn'>X</button>";
    }
  }
}

jQuery('#add-type-button').click(function() {
  jQuery('#add-type-text').hide();
  jQuery('#add-type-button').hide();
  var typeId = jQuery("#type-selected-value").val();
  var typeLabel = jQuery("#type-selected").val();
  var domainType = typeId+":"+getDomainForType(typeId)[1]+":"+typeLabel;

  curTypeList.push(domainType);
  var typeDomainLabels = curTypeList.slice();
  for(var elem of curEndTypeList) {
    if(typeDomainLabels.includes(elem) == false) {
        typeDomainLabels.push(elem);
    }
  }
  showDtlTable("dtl", typeDomainLabels);

  //before updating DL-TL-EL make sure the domain and type selections are removed, because only nodtypevalues are required
  jQuery("#domain-options option:selected").attr('selected', false);
  jQuery("#type-options option:selected").attr('selected', false);

  updateDomainsTypesEntities("", "");
  domains = generatedDomains.slice();
  types = generatedTypes.slice();
  entities = generatedEntities.slice();
  displayDomainOptions(domains);
  displayTypeOptions(types);
  displayEntityOptions(entities);

  prevTypSel = null;

  // var dtl = document.getElementById("dtl");
  // var row = dtl.insertRow(-1);
  // var cell1 = row.insertCell(0);
  // var cell2 = row.insertCell(1);
  // var cell3 = row.insertCell(2);
  // var domain = getDomainForType(typeId)[1].toUpperCase();
  // var type = typeLabel.toUpperCase();
  // cell1.innerHTML = "<i>"+domain+"</i>";
  // cell2.innerHTML = type;
  // cell3.innerHTML = "<button class='editbtn'>X</button>";


  //
  //
  // jQuery('#add-type-button').hide();
  // var typeId = jQuery("#type-selected-value").val();
  // var typeLabel = jQuery("#type-selected").val();
  // var domainType = typeId+":"+getDomainForType(typeId)[1]+":"+typeLabel;
  // if(curTypeList.includes(domainType) == false) {
  //   curTypeList.push(domainType);
  // }
  // var typeIds = [];
  // var typeDomainLabels = [];
  // for(var elem of curTypeList) {
  //   if(typeIds.includes(elem.split(':')[0]) == false) {
  //     typeIds.push(elem.split(':')[0]);
  //     typeDomainLabels.push(elem.split(':').slice(1,3).join(':'));
  //   }
  // }
  // jQuery("#type-list").html(tdListText+typeDomainLabels.join(', '));
});

jQuery('#edit-add-type-button').click(function() {
  jQuery('#edit-add-type-text').hide();
  jQuery('#edit-add-type-button').hide();
  var typeId = jQuery("#node-edit-type-options option:selected").val();
  var typeLabel = jQuery("#node-edit-type-options option:selected").text().toLowerCase();
  var domainType = typeId+":"+getDomainForType(typeId)[1]+":"+typeLabel;

  curTypeList.push(domainType);
  var typeDomainLabels = curTypeList.slice();
  for(var elem of curEndTypeList) {
    typeDomainLabels.push(elem);
  }
  showDtlTable("edit-dtl", typeDomainLabels);
  // var dtl = document.getElementById("edit-dtl");
  // var row = dtl.insertRow(-1);
  // var cell1 = row.insertCell(0);
  // var cell2 = row.insertCell(1);
  // var cell3 = row.insertCell(2);
  // cell1.innerHTML = "<i>"+getDomainForType(typeId)[1].innerHTML.toUpperCase()+"</i>";
  // cell2.innerHTML = typeLabel.innerHTML.toUpperCase();
  // cell3.innerHTML = "<button class='editbtn'>X</button>";


  //showDtlTable("edit-dtl", typeDomainLabels);
  // var domainTypeList = curTypeList.join(', ');
  //
  // if(Array.isArray(curEndTypeList) && curEndTypeList.length) {
  //   if(domainTypeList != "") {
  //     domainTypeList += ", ";
  //   }
  //   domainTypeList += curEndTypeList.join(', ');
  // }
  // var typeIds = [];
  // var typeDomainLabels = [];
  // for(var elem of domainTypeList.split(', ')) {
  //   if(typeIds.includes(elem.split(':')[0]) == false) {
  //     typeIds.push(elem.split(':')[0]);
  //     typeDomainLabels.push(elem.split(':').slice(1,3).join(':'));
  //  }
  // }
  // jQuery("#edit-type-list").html("This node currently belongs to the following Types (and the corresponding Domains):");
  // showDtlTable("edit-dtl", typeDomainLabels);
});


function resetMouseVars() {
    mousedown_node = null;
    mouseup_node = null;
    mousedown_link = null;
}
var translateAllowed = false;

jQuery("#rearrange-button").click(function() {
    force.start();
    if (translateAllowed == false) {
        translateAllowed = true;
        allowNodeDrag = true;
        //   nodeclick = false;
        drawEdge = false;
    } else {
        translateAllowed = false;
        allowNodeDrag = false;
        //   nodeclick = true;
        drawEdge = true;
    }
});
// jQuery("#rearrange-button").on("mousedown",function(){
//   jQuery("#switch-button").text(function(){
//   if (translateAllowed == false)
//   return "Lock the Graph"
//   else
//   return "Rearrange the Graph"
// });
//   if (translateAllowed == false){
//   translateAllowed = true;
//   allowNodeDrag = true;
// //   nodeclick = false;
//   drawEdge = false;
//   }
//   else{
//     translateAllowed = false;
//   allowNodeDrag = false;
// //   nodeclick = true;
//   drawEdge = true;
//   }
// });
// update force layout (called automatically each iteration)
function tick() {

    var width = jQuery("#queryConstructSVG").width();
    var height = jQuery("#queryConstructSVG").height();
    circle.attr("cx", function(d) {
            return d.x = Math.max(radius, Math.min(width - radius, d.x));
        })
        .attr("cy", function(d) {
            return d.y = Math.max(radius, Math.min(height - radius, d.y));
        });
    // draw directed edges with proper padding from node centers
    path.attr('d', function(d) {

        if (d == null) {
            return "M0,0A0,0 0 0,1 0,0";
        }
        var dx = d.target.x - d.source.x,
            dy = d.target.y - d.source.y,
            dr = Math.sqrt(dx * dx + dy * dy);
        if (isNaN(d.source.x) || isNaN(d.source.y) || isNaN(dr) || isNaN(d.target.x) || isNaN(d.target.y) ) {
            return "M0,0A0,0 0 0,1 0,0";
        }
        var list_links = [];
        for (var i = 0; i < links.length; i++) {
            if(links[i].source.id != links[i].target.id){
            
                if ((d.source == links[i].source && d.target == links[i].target) || (d.source == links[i].target && d.target == links[i].source)) {
                    list_links.push(links[i]);
                }
            }
        }
        if (list_links.length <= 1) {
            if (d.source.x < d.target.x) {
                return "M" + d.source.x + "," + d.source.y + "A" + dr + "," + dr + " 0 0,1 " + d.target.x + "," + d.target.y;
            } else {
                return "M" + d.target.x + "," + d.target.y + "A" + dr + "," + dr + " 0 0,1 " + d.source.x + "," + d.source.y;
            }
        } else {
            if (list_links.indexOf(d) % 2 == 0) {
                // return "M" + d.source.x + "," + d.source.y + "A" + dr + "," + dr + " 0 0,1 " + d.target.x + "," + d.target.y;
                if (d.source.x < d.target.x) {
                    return "M" + d.source.x + "," + d.source.y + "A" + dr + "," + dr + " 0 0,1 " + d.target.x + "," + d.target.y;
                } else {
                    return "M" + d.target.x + "," + d.target.y + "A" + dr + "," + dr + " 0 0,1 " + d.source.x + "," + d.source.y;
                }
            } else {
                // return "M" + d.target.x + "," + d.target.y + "A" + dr + "," + dr + " 0 0,1 " + d.source.x + "," + d.source.y;
                if (d.source.x > d.target.x) {
                    return "M" + d.source.x + "," + d.source.y + "A" + dr + "," + dr + " 0 0,1 " + d.target.x + "," + d.target.y;
                } else {
                    return "M" + d.target.x + "," + d.target.y + "A" + dr + "," + dr + " 0 0,1 " + d.source.x + "," + d.source.y;
                }
            }
        }
        //     var deltaX = d.target.x - d.source.x,
        //         deltaY = d.target.y - d.source.y,
        //         //dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        //         dist = 300/d.linkNum;
        //         normX = deltaX / dist,
        //         normY = deltaY / dist,
        //         sourcePadding = 30,
        //         targetPadding = 30,
        //         sourceX = d.source.x + (sourcePadding * normX),
        //         sourceY = d.source.y + (sourcePadding * normY),
        //         targetX = d.target.x - (targetPadding * normX),
        //         targetY = d.target.y - (targetPadding * normY);
        //     //return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
        //       return 'M' + d.source.x + ',' + d.source.y + 'A' + dist + ',' + dist + ' 0 0,1 ' + d.target.x + ',' + d.target.y;

        //      // return 'M' + d.source.x + ',' + d.source.y + 'A' + dist + ',' + dist + ' 0 0,1 ' + targetX + ',' + targetY;
    });

    circle.attr('transform', function(d) {
        if (!isNaN(d.x) && !isNaN(d.y))
            return 'translate(' + d.x + ',' + d.y + ')';

    });
    // circle.attr("fill", "None");

    //   if(mousedown_node==null && drawEdge){
    //       for(var i=0;i<1;i++){
    //         force.tick();
    //       }
    //   }

}

function getTypeValues(typeList) {
  var typeIds = "";
  if (typeList != undefined) {
    for (x of typeList) {
      if (typeIds != "") {
        typeIds += ",";
      }
      typeIds += x.split(':')[0];
    }
  }
  return typeIds;
}

// restart starts here
// update graph (called when needed)
function restart() {
    // console.log("inside restart function");
    // path (link) group

    if (suggestionsFinalized == true) {
        for (var i = 0; i < nodes.length; i++) {
            if (nodes[i].greyflag == 2) {
                nodes[i].greyflag = 0
            };
        }
        suggestionsFinalized = false;
    }



    for (var i = 0; i < links.length; i++) {
        var ID = "#edge" + (links[i].id);
        jQuery(ID).remove();
    };
    for (var i = 0; i < links.length; i++) {

        var id = "#linkLabelHolderId_" + (links[i].id);
        jQuery(id).remove();
    };

    value.length = 0;
    for (var i = 0; i < links.length; i++) {
        value[i] = links[i].value;
        if (links[i].flag == 20 && links[i].source != links[i].target) {
            jQuery("#linkId_" + links[i].id).attr("stroke-dasharray", "10,10");
            jQuery("#linkId_" + links[i].id).css('stroke', '#9E9E9E');
            if (links[i] == selected_link) {
                jQuery("#linkId_" + links[i].id).css('stroke', '#58faf4');
            }
        } else if ((links[i].flag == 2 || links[i].flag == 20) && links[i].source.flag == 1 && links[i].source.greyflag == 0 && links[i].target.flag == 1 && links[i].target.greyflag == 0 && links[i].source != links[i].target) {
            jQuery("#linkId_" + links[i].id).attr("stroke-dasharray", "0,0");
            jQuery("#linkId_" + links[i].id).css('stroke', 'black');

        } else if (((links[i].flag == 2 || links[i].flag == 20) && links[i].source.flag == 2 && links[i].source.greyflag == 1 && links[i].target.flag == 1 && links[i].target.greyflag == 0) || (links[i].flag == 2 && links[i].target.flag == 2 && links[i].target.greyflag == 1 && links[i].source.flag == 1 && links[i].source.greyflag == 0 && links[i].source != links[i].target)) {
            jQuery("#linkId_" + links[i].id).attr("stroke-dasharray", "10,10");
            jQuery("#linkId_" + links[i].id).css('stroke', '#9E9E9E');
        } else if (((links[i].flag == 2 || links[i].flag == 20) && links[i].source.flag == 1 && links[i].source.greyflag == 2 && links[i].target.flag == 1 && links[i].target.greyflag == 0) || (links[i].flag == 2 && links[i].target.flag == 1 && links[i].target.greyflag == 2 && links[i].source.flag == 1 && links[i].source.greyflag == 0 && links[i].source != links[i].target)) {
            jQuery("#linkId_" + links[i].id).attr("stroke-dasharray", "10,10");
            jQuery("#linkId_" + links[i].id).css('stroke', '#9E9E9E');
        } else if ((links[i].flag == 3 || links[i].flag == 4) && links[i].source != links[i].target) {
            jQuery("#linkId_" + links[i].id).css('stroke-width', '6px');
        } else if (links[i].flag == 10 && links[i].source != links[i].target) {
            jQuery("#linkId_" + links[i].id).css('stroke-width', '3px');
            jQuery("#linkId_" + links[i].id).css('stroke', '#96FEFB');
        } else if (links[i].flag == 9 && links[i].source != links[i].target) {
            jQuery("#linkId_" + links[i].id).css('stroke-width', '3px');
            jQuery("#linkId_" + links[i].id).css('stroke', '#D0D0D0');
        }
    };


    if (linkSuggestionsFinalized == true) {
        for (var i = 0; i < links.length; i++) {
            if (links[i].flag == 4) {
                jQuery("#linkId_" + links[i].id).css('stroke', 'black');
                jQuery("#linkId_" + links[i].id).css('stroke-width', '3px');
                links[i].flag = 2;
            }
        }
        linkSuggestionsFinalized = false;
    }



    jQuery('#defs-marker').empty();
    defs.data(value)
        .enter();

    path = path.data(links);

    path.enter().append("svg:path")
        .attr("class", function(d) {
            return ("link " + d.value);
        })
        .attr("id", function(d, i) {
            return "linkId_" + d.id;
        })
        .attr("marker-end", function(d) {
            return "url(#" + d.value + ")";
        })
        .style('stroke', function(d) {
            if ((d.flag == 2 && d.source.flag == 2 && d.source.greyflag == 1 && d.target.flag == 1 && d.target.greyflag == 0) || (d.flag == 2 && d.target.flag == 2 && d.target.greyflag == 1 && d.source.flag == 1 && d.source.greyflag == 0))
                return "#9E9E9E";
            else if (d.flag == 3)
                return "#B0B0B0";
            else if (d.flag == 4)
                return "#2EFEF7";
            else if (d.flag == 10)
                return "#96FEFB";
            else if (d.flag == 9)
                return "#D0D0D0";
            else
                return 'black';
        })
        //       .on('mousedown', function(d) {
        //           if (selected_link == null || selected_link != d) {
        //             if (selected_link){
        //                 if (selected_link.flag != 2){
        //                     if (selected_link.flag == 3){
        //                     jQuery("#linkId_"+selected_link.id).css('stroke', '#B0B0B0');}
        //                     else if (selected_link.flag == 4){
        //                     jQuery("#linkId_"+selected_link.id).css('stroke', '#2EFEF7');}
        //                 }
        //                 else {jQuery("#linkId_"+selected_link.id).css('stroke', 'black');}
        //             }
        //             selected_link = d;
        //             if (selected_link.flag != 2){
        //                jQuery("#linkId_"+d.id).css('stroke', '#2EFEF7');
        //             }
        //             else{var i = 0;}
        //             //Orange removed //else{jQuery("#linkId_"+d.id).css('stroke', 'orange');}
        //             // d.style("fill", "green");
        //           }else if(selected_link == d){
        //             if (selected_link.flag == 3){
        //                jQuery("#linkId_"+selected_link.id).css('stroke', '#B0B0B0');}
        //              else if (selected_link.flag == 4){
        //                jQuery("#linkId_"+selected_link.id).css('stroke', '#2EFEF7');}
        //             else {jQuery("#linkId_"+selected_link.id).css('stroke', 'black');}
        //             selected_link = null
        //           }

        //           allowNodeCreation = false;

        //           jQuery("#edge-options").empty();
        //           for (var i = 0; i < linkTypes.length; i++) {
        //             jQuery("#edge-options").append('<option value="'+linkTypes[i]+'" id="add-value-1" onClick=changeName('+linkTypes[i]+')>'+linkTypes[i]+'</option>');
        //           };

        //           if(d3.event.ctrlKey) return;

        //           // select link
        //           mousedown_link = d;
        //           // if(mousedown_link === selected_link) selected_link = null;
        //           // else selected_link = mousedown_link;
        //           selected_node = null;

        //           console.log("link selected");
        //           // selectEgde();
        //           jQuery("#type-div").hide();
        //           jQuery("#entity-div").hide();
        //           jQuery("#domain-div").hide();
        //           jQuery("#keyword-div").hide();
        //           jQuery("#type-border").hide();
        //           jQuery("#entity-border").hide();
        //           jQuery("#selected-div").hide();
        //           jQuery("#type-keyword-div").hide();
        //           jQuery("#edge-div").show();
        //           link_click=true;
        //           restart();
        //         })
        .on('mouseup', function(d) {
            if (selected_link) {
                if (selected_link.flag != 2) {
                    jQuery("#linkId_" + d.id).css('stroke', '#2EFEF7');
                }
                if (selected_link.flag == 20) {
                    jQuery("#linkId_" + d.id).css('stroke', '#58faf4');
                } else {
                    var i = 0;
                }
                // Orange removed //else{jQuery("#linkId_"+d.id).css('stroke', 'orange');}
            } else {
                if (d.flag == 3) {
                    jQuery("#linkId_" + d.id).css('stroke', '#B0B0B0');
                } else if (d.flag == 4) {
                    jQuery("#linkId_" + d.id).css('stroke', '#2EFEF7');
                } else {
                    jQuery("#linkId_" + d.id).css('stroke', 'black');
                }
            }
            allowNodeCreation = true;
            restart();

            if (selected_link) {
                partialGraph.length = 0;
                for (var i = 0, j = 0; i < links.length; i++) {
                    if (links[i].linkID != -1 && (links[i].source != links[i].target) && (links[i].source.greyflag == 0 || links[i].target.greyflag == 0)) {
                        partialGraph[j] = {
                            source: links[i].source.nodeID,
                            graphSource: links[i].source.id,
                            edge: links[i].linkID,
                            object: links[i].target.nodeID,
                            graphObject: links[i].target.id,
                            sourceTypeValues: getTypeValues(links[i].source.typeList)+","+getTypeValues(findEdgeEndTypes(links[i].source)),
                            objectTypeValues: getTypeValues(links[i].target.typeList)+","+getTypeValues(findEdgeEndTypes(links[i].target)),
                            sourceEntity: links[i].source.entity,
                            objectEntity: links[i].target.entity
                        };
                        j++;
                    }
                };


                for (var l = 0, k = partialGraph.length; l < nodes.length; l++) {
                    var checklink = false;
                    for (var i = 0; i < links.length; i++) {
                        if ((nodes[l] == links[i].source || nodes[l] == links[i].target) && links[i].source != links[i].target) {
                            checklink = true;
                        }
                    }
                    if (!checklink) {
                        partialGraph[k] = {
                            source: nodes[l].nodeID,
                            graphSource: nodes[l].id,
                            edge: -1,
                            object: 0,
                            graphObject: -1,
                            sourceTypeValues: getTypeValues(nodes[l].typeList)+","+getTypeValues(findEdgeEndTypes(nodes[l])),
                            objectTypeValues: "",
                            sourceEntity: nodes[l].entity,
                            objectEntity: -1
                        };
                        k++;
                    }
                }

                var data = {
                    partialGraph: partialGraph,
                    nodes: nodes,
                    mode: 0,
                    rejectedGraph: rejectedGraph,
                    activeEdgeEnds: {
                        source: selected_link.source.nodeID,
                        object: selected_link.target.nodeID,
                        graphSource: selected_link.source.id,
                        graphObject: selected_link.target.id
                    },
                    dataGraphInUse: 0,
                    topk: noOfSuggestions,
                    refreshGraphNode: 0
                };
                // postRequest(data, linkTypes, returnObject);
                var edgeMode = 3;
                postRequest(data, edgeMode);
            };
        });



    // remove old links
    // path.exit().remove();



    linkText.data(force.links())
        .enter().append("g").attr("class", "linklabelholder")
        .attr("id", function(d, i) {
            return "linkLabelHolderId_" + d.id;
        })
        .append("text")
        .attr("class", "linklabel")
        .style("font-size", "13px") //Changed by Heet
        //          .attr('x', "35") Changed by Heet
        .attr("dy", "-5") //Changed by Heet
        //          .attr("text-anchor", "middle")
        .attr("text-anchor", function(d) {
            if (d.value.length > 35) {
                return "start"
            } else return "middle"
        })
        // .attr("textLength", "50px")
        .attr("textLength", function(d) {
            if (d.value.length > 33) {
                return "200px";
            } else return d.value.length*5+"px";
        })
        .attr("lengthAdjust", "spacingAndGlyphs")
        //.style("fill","#000")
        .style("fill", function(d, i) {
            if (d.flag == 10) {
                return "#58FAF4"
            } else if (d.flag == 9) {
                return "#D8D8D8"
            } else {
                return "#000"
            }
        })
        .append("textPath").attr('id', function(d, i) {
            return "textPath" + d.id;
        })
        // .attr("id", function(d,i) { return "#linkId_" + d.id;})
        //          .attr("startOffset", "50%") //Changed by Heet
        .attr("startOffset", function(d) {
            if (d.value.length > 35) {
                return "43px"
            } else return "50%"
        })
        //          .append("title").text("xlink:href", function(d,i) { return "#linkId_" + d.id;})
        //          .append("title").text(function(d){return d.value})
        .attr("xlink:href", function(d, i) {
            return "#linkId_" + d.id;
        })
        .text(function(d) {
            if (d.value.length >= 30) {
                return d.value.replace(/_/g, " ").substring(0, 32) + ".....";
            } else {
                return d.value.replace(/_/g, " ");
            }
        }).on('mousedown', function(d) {
            /*if (greyLinkSelectedFlag >= 1 && greyLinkSelected == d)
            {
                greyLinkSelectedFlag = 2;}*/
            if (selected_link == null || selected_link != d) {
                if (d.target.greyflag == 1 || d.source.greyflag == 1) {
                    /*if (greyLinkSelectedFlag >= 1 && greyLinkSelected == d){
                       greyLinkSelectedFlag = 2;} else{*/
                    greyLinkSelectedFlag = 1;
                    // }
                    greyLinkSelected = d;
                }
                if (selected_link) {
                    if (selected_link.flag == 3) {
                        jQuery("#linkId_" + selected_link.id).css('stroke', '#B0B0B0');
                    } else if (selected_link.flag == 4) {
                        jQuery("#linkId_" + selected_link.id).css('stroke', '#2EFEF7');
                    } else {
                        jQuery("#linkId_" + selected_link.id).css('stroke', 'black');
                    }
                }
                selected_link = d;
                if (selected_link != null && selected_link.flag != 2) {

                    for (var k = 0; k < links.length; k++) {
                        if (d === links[k]) {
                            if (d.flag == 3) {
                                links[k].flag = 4;
                                selected_link.flag = 4;
                                greyLinkSuggestionsPicked++;
                                greyLinkPickdPerSubSuggestion++;
                                if (greyLinkPickdPerSubSuggestion == 1 && SugPickdPerGreyLinkCount == 0) {
                                    displayFlag = 1;
                                }
                                animatedHelp();
                            } else if (d.flag == 4) {
                                links[k].flag = 3;
                                selected_link.flag = 3;
                                greyLinkSuggestionsPicked--;
                                greyLinkPickdPerSubSuggestion--;
                                animatedHelp();
                                if (greyLinkSuggestionsPicked == 0 && noOfSuggestionsPicked == 0 && subSuggestionsInProgressFlag == false) {
                                    jQuery("#submit-button").attr("class", "menu-border");
                                    jQuery("#button-g").attr("class", "menu-border");
                                }
                            }
                            break;
                        }
                    }
                    if (selected_link.flag == 4) {
                        jQuery("#linkId_" + d.id).css('stroke', '#2EFEF7');
                    } else if (selected_link.flag == 3) {
                        jQuery("#linkId_" + d.id).css('stroke', '#B0B0B0');
                    } else {
                        jQuery("#linkId_" + d.id).css('stroke', 'black');
                    }

                } else {
                    //Orange removed// jQuery("#linkId_"+d.id).css('stroke', 'orange');
                    if (selected_link.flag == 2 && selected_link.source != selected_link.target && selected_link.source.greyflag == 0 && selected_link.target.greyflag == 0 && suggestions == false && suggestionsFinalized == false && linkSuggestionsFinalized == false) {
                        jQuery("#linkId_" + d.id).css('stroke', 'orange');
                        getExamples(selected_link);
                    } else if (selected_link.flag == 2 && selected_link.source != selected_link.target && selected_link.source.greyflag == 0 && selected_link.target.greyflag == 0 && addEdgeButtonEdgeAdded == true && (exampleSource != selected_link.source || exampleTarget != selected_link.target || exampleLinkId != selected_link.linkID)) {
                        jQuery("#linkId_" + exampleLinkGraphId).css('stroke', 'black');
                        for (var y = 0; y < nodes.length; y++) {
                            if (nodes[y].greyflag == 2 && nodes[y].flag == 1) {
                                nodes[y].greyflag = 0;
                                nodes[y].flag = 1;
                            }
                        }
                        removeTempNodes();
                        suggestions = false;
                        suggestionPicked = false;
                        exampleSource = null;
                        exampleTarget = null;
                        exampleLinkId = -1;
                        exampleLinkGraphId = -1;
                        exampleReverseRoleFlag = false;
                        animatedHelp();
                        exampleActualSourceType = null;
                        exampleActualTargetType = null;
                        addEdgeButtonEdgeAdded = false;
                        jQuery("#Add-Edge-button").hide();
                        jQuery("#example-text").show();
                        jQuery("#active-examples").empty();
                        jQuery("#Reverse-button").hide();
                        jQuery("#linkId_" + d.id).css('stroke', 'orange');
                        jQuery("#submit-button").attr("class", "menu-border");
                        jQuery("#button-g").attr("class", "menu-border");
                        getExamples(selected_link);
                    }
                }
                // d.style("fill", "green");
            } else if (selected_link == d) {
                if (d.flag != 2) {
                    for (var k = 0; k < links.length; k++) {
                        if (d === links[k]) {
                            if (d.flag == 3) {
                                links[k].flag = 4;
                                selected_link.flag = 4;
                                greyLinkSuggestionsPicked++;
                                greyLinkPickdPerSubSuggestion++;
                                if (greyLinkPickdPerSubSuggestion == 1 && SugPickdPerGreyLinkCount == 0) {
                                    displayFlag = 1;
                                }
                                animatedHelp();
                            } else if (d.flag == 4) {
                                links[k].flag = 3;
                                selected_link.flag = 3;
                                greyLinkSuggestionsPicked--;
                                greyLinkPickdPerSubSuggestion--;
                                animatedHelp();
                                if (greyLinkSuggestionsPicked == 0 && noOfSuggestionsPicked == 0 && subSuggestionsInProgressFlag == false) {
                                    jQuery("#submit-button").attr("class", "menu-border");
                                    jQuery("#button-g").attr("class", "menu-border");
                                }
                            }
                            break;
                        }
                    }
                }
                if (selected_link.flag == 3) {
                    jQuery("#linkId_" + selected_link.id).css('stroke', '#B0B0B0');
                } else if (selected_link.flag == 4) {
                    jQuery("#linkId_" + selected_link.id).css('stroke', '#2EFEF7');
                } else {
                    jQuery("#linkId_" + selected_link.id).css('stroke', 'black');
                    if ((exampleReverseRoleFlag == true && addEdgeButtonEdgeAdded == false) || (exampleReverseRoleFlag == true && addEdgeButtonEdgeAdded == true && exampleSource === selected_link.source && exampleTarget === selected_link.target && exampleLinkId == selected_link.linkID)) {
                        jQuery("#linkId_" + exampleLinkGraphId).css('stroke', 'black');
                        if (addEdgeButtonEdgeAdded == true) {
                            for (var y = 0; y < nodes.length; y++) {
                                if (nodes[y].greyflag == 2 && nodes[y].flag == 1) {
                                    nodes[y].greyflag = 0;
                                    nodes[y].flag = 1;
                                }
                            }
                            removeTempNodes();
                            suggestions = false;
                            suggestionPicked = false;
                        }
                        exampleSource = null;
                        exampleTarget = null;
                        exampleLinkId = -1;
                        exampleLinkGraphId = -1;
                        exampleReverseRoleFlag = false;
                        animatedHelp();
                        exampleActualSourceType = null;
                        exampleActualTargetType = null;
                        addEdgeButtonEdgeAdded = false;
                        jQuery("#Add-Edge-button").hide();
                        jQuery("#example-text").show();
                        jQuery("#active-examples").empty();
                        jQuery("#Reverse-button").hide();
                    }
                }
                selected_link = null
            }

            allowNodeCreation = false;

            jQuery("#edge-options").empty();
            for (var i = 0; i < linkTypes.length; i++) {
                jQuery("#edge-options").append('<option value="' + linkTypes[i] + '" id="add-value-1" onClick=changeName(' + linkTypes[i] + ')>' + linkTypes[i] + '</option>');
            };

            if (d3.event.ctrlKey) return;

            // select link
            mousedown_link = d;
            // if(mousedown_link === selected_link) selected_link = null;
            // else selected_link = mousedown_link;
            selected_node = null;
            console.log("link selected");
            // selectEgde();
            // jQuery("#type-div").hide();
            // jQuery("#entity-div").hide();
            // jQuery("#domain-div").hide();
            // jQuery("#edge-div").show();
            link_click = true;
            restart();
            if (greyLinkSelectedFlag == 1) {
                getSubSuggestions();
                greyLinkSelectedFlag = 0;

            }
        })
        .on('mouseup', function(d) {
            if (selected_link) {
                if (selected_link.flag == 4) {
                    jQuery("#linkId_" + d.id).css('stroke', '#2EFEF7');
                } else if (selected_link.flag == 3) {
                    jQuery("#linkId_" + d.id).css('stroke', '#B0B0B0');
                } else {
                    //Orange removed //jQuery("#linkId_"+d.id).css('stroke', 'orange');
                    if (selected_link.source != selected_link.target && selected_link.source.greyflag == 0 && selected_link.target.greyflag == 0 && suggestions == false && suggestionsFinalized == false && linkSuggestionsFinalized == false) {
                        jQuery("#linkId_" + d.id).css('stroke', 'orange');
                        getExamples(selected_link);
                    } else if (selected_link.flag == 2 && selected_link.source != selected_link.target && selected_link.source.greyflag == 0 && selected_link.target.greyflag == 0 && addEdgeButtonEdgeAdded == true && (exampleSource != selected_link.source || exampleTarget != selected_link.target || exampleLinkId != selected_link.linkID)) {
                        jQuery("#linkId_" + exampleLinkGraphId).css('stroke', 'black');
                        for (var y = 0; y < nodes.length; y++) {
                            if (nodes[y].greyflag == 2 && nodes[y].flag == 1) {
                                nodes[y].greyflag = 0;
                                nodes[y].flag = 1;
                            }
                        }
                        removeTempNodes();
                        suggestions = false;
                        suggestionPicked = false;
                        exampleSource = null;
                        exampleTarget = null;
                        exampleLinkId = -1;
                        exampleLinkGraphId = -1;
                        exampleReverseRoleFlag = false;
                        animatedHelp();
                        exampleActualSourceType = null;
                        exampleActualTargetType = null;
                        addEdgeButtonEdgeAdded = false;
                        jQuery("#example-text").show();
                        jQuery("#Add-Edge-button").hide();
                        jQuery("#active-examples").empty();
                        jQuery("#Reverse-button").hide();
                        jQuery("#linkId_" + d.id).css('stroke', 'orange');
                        jQuery("#submit-button").attr("class", "menu-border");
                        jQuery("#button-g").attr("class", "menu-border");
                        getExamples(selected_link);
                    }
                }
            } else {
                if (d.flag == 3) {
                    jQuery("#linkId_" + d.id).css('stroke', '#B0B0B0');
                } else if (d.flag == 4) {
                    jQuery("#linkId_" + d.id).css('stroke', '#2EFEF7');
                } else {
                    jQuery("#linkId_" + d.id).css('stroke', 'black');
                }
            }
            allowNodeCreation = true;

            if (selected_link) {
                // for (var i = 0, j=0; i < links.length; i++) {
                //   if (links[i].linkID != -1 && (links[i].source != links[i].target)) {
                //     partialGraph[j] = {source: links[i].source.nodeID, edge: links[i].linkID, object: links[i].target.nodeID};
                //     j++;
                //   }
                // };

                // var data = {
                //   partialGraph: partialGraph,
                //   mode: 0,
                //   rejectedGraph: rejectedGraph,
                //   activeEdgeEnds: {source: selected_link.source.nodeID, object: selected_link.target.nodeID},
                //   dataGraphInUse: 0
                // };
                // postRequest(data);
                selected_node = null;
                // jQuery("#myModal").modal('show');
                // jQuery("#type-div").hide();
                // jQuery("#entity-div").hide();
                // jQuery("#domain-div").hide();
                // jQuery("#edge-options").empty();
                // for (var i = 0; i < linkTypes.length; i++) {
                //   jQuery("#edge-options").append('<option value="'+linkTypes[i][0]+'" id="add-value-1" >'+linkTypes[i][1]+'</option>');
                // };
                // jQuery("#edge-div").show();
            };

            restart();
        })
        //         .on("mousemove", function(d){
        //             if (mouseOverEdgeFlag == true){
        //               return tooltipEdgeExample.style("top", (event.pageY)+"px").style("left",(event.pageX)+"px");
        //             }
        //          })
        .on("mouseout", function(d) {
            selected_link = d;
            jQuery("#node" + d.source.id).css("fill", "#000000");
            jQuery("#node" + d.target.id).css("fill", "#000000");
            exampleReverseRoleFlag = false;
            if (mouseOverEdgeFlag == true) {
                mouseOverEdgeFlag = false;
                jQuery("#linkId_" + d.id).css('stroke', '#9E9E9E');
                //                 force.start();
                force.alpha(alpha);
                return tooltipEdgeExample.style("visibility", "hidden");
            }
        })
        .on('mouseover', function(d) {
            if (!translateAllowed) {
                this.style.cursor = 'pointer';
                selected_link = d;
                alpha = force.alpha();
                force.stop();
                tooltipEdgeExample.style("visibility", "visible");
                tooltipEdgeExample.style("top", (event.pageY - 60) + "px").style("left", (event.pageX - 20) + "px");
                //             if(d.source.greyflag==0)
                jQuery("#node" + d.source.id).css("fill", "#0047ab");
                //              if(d.target.greyflag==0){
                jQuery("#node" + d.target.id).css("fill", "#00CC00");
                var sourceNodeId = d.source.nodeID;
                var targetNodeId = d.target.nodeID;
                var edgeId = d.linkID;
                var responseEdge;
                //             getRequest("https://" + urlFull + "/orion_app/getexamples?edge="+edgeId+"&source="+sourceNodeId+"&object="+targetNodeId, response);
                jQuery.ajax({
                    url: "https://" + urlFull + "/orion_app/getexamples?edge=" + edgeId + "&source=" + sourceNodeId + "&object=" + targetNodeId,
                    type: 'get',
                    async: false,
                    success: function(r) {
                        responseEdge = r;
                    }
                });
                responseExample = [];
                jQuery.ajax({
                    url: "https://xxx.uta.edu/orion_app/getedgepreview?edgeid=" + edgeId,
                    type: 'get',
                    async: false,
                    success: function(r) {
                        responseExample = r;
                    }
                });
                jQuery("#tooltip-examples").empty();

                var typeSource;
                var typeObject;
                if (responseEdge.sourceType == null && responseEdge.objectType == null) {
                    typeSource = d.source.name.replace(/_/g, " ");
                    typeObject = d.target.name.replace(/_/g, " ");
                } else if (responseEdge.sourceType == null) {
                    typeSource = d.source.name.replace(/_/g, " ");
                    typeObject = responseEdge.objectType.split(',')[1].replace(/_/g, " ");
                } else if (responseEdge.objectType == null) {
                    typeObject = exampleTarget.name.replace(/_/g, " ");
                    typeSource = responseEdge.sourceType.split(',')[1].replace(/_/g, " ");
                } else {
                    typeSource = responseEdge.sourceType.split(',')[1].replace(/_/g, " ");
                    typeObject = responseEdge.objectType.split(',')[1].replace(/_/g, " ");
                }
                jQuery("#tooltip-examples").append('<li class="" style="font-size: 15px; list-style-type:none;padding:0px; margin:0px"><span style="font-size: 16px;">Edge Label:</span> ' + d.value.replace(/_/g, " ") + '</li>');
                //                 jQuery("#tooltip-examples").append('<br>'); //Edited by AD
                jQuery("#tooltip-examples").append('<li class="" style="font-size: 15px; list-style-type:none;padding:0px; margin:0px"><span style="font-size: 16px;">Label Description:</span> The edge connects the node of type <span style="color:#0047ab" id="sourceType"></span>  to the node of type </span><span id="targetType" style="color:#00CC00;"></span>.</li>'); //Edited by AD
                //                 jQuery("#tooltip-examples").append('<br>');
                jQuery("#tooltip-examples").append('<li class="" style="font-size: 15px; list-style-type:none;padding:0px; margin:0px"><span style="font-size: 16px;">Example:</span> <span style="color:#0047ab" id="sourceTypeEx"></span> <span style="color:#063145;"> &#8594 </span><span id="targetTypeEx" style="color:#00CC00;"></span></li>');
                jQuery("#tooltip-examples").append('<br>');
                //                 if((d.source.greyflag==0 || d.source.flag==1) && (d.target.greyflag==0 || d.target.flag == 1)){
                //                 jQuery("#button-container").append('<div style="text-align:center;float:left;"><button id="linkDel" style="text-align:center; border: solid black;"></button></div>');
                //                 }else{
                //                   jQuery("#tooltip-examples").append('<div style="text-align:center;"><button id="linkAdd" style="text-align:center; border: solid black;">Add</button></div>');
                //                 }
                if (d.source.greyflag == 0 && d.target.greyflag == 0 && d.flag != 20) {
                    // if(selected_link.source.greyflag==0 && selected_link.target.greyflag==0 && selected_link.flag != 20){
                    jQuery("#linkDel").text("Delete Edge");
                } else {
                    jQuery("#linkDel").text("Add Edge");
                }
                // selected_link = d;



                jQuery("#linkDel").click(function() {
                    if (selected_link) {
                        if (selected_link.source.greyflag == 0 && selected_link.target.greyflag == 0 && selected_link.flag != 20) {
                            jQuery("#query-modal-body").hide();
                            jQuery("#no-result-modal-body").hide();
                            jQuery("#no-query-modal-body").show();
                            updateUndo();
                            force.start();
                            tooltipEdgeExample.style("visibility", "hidden");
                            //     jQuery("#linkDel").text("Delete");
                            for (i = 0; i < links.length; i++) {
                                if (selected_link.id == links[i].id) {
                                    links[i].source = links[i].target;
                                    break;
                                }
                            }
                            tooltipEdgeExample.style("visibility", "hidden");
                            //   forLinks = 1;
                            //removeTempNodes();
                          
                            jQuery('#loading').modal('show');
                            jQuery('#loading-indicator').show();
                            setTimeout(function() {
                                restart();  
                                jQuery('#loading-indicator').hide();
                                jQuery('#loading').modal('hide');
                            },500)

                            //restart();

                            // selected_link = d;tooltipEdgeExample.style("visibility", "visible");
                        } else {
                            updateUndo();
                            //    jQuery("#linkDel").text("Add");
                            tooltipEdgeExample.style("visibility", "hidden");
                            for (i = 0; i < links.length; i++) {
                                if (selected_link.id == links[i].id) {
                                    links[i].source.flag = 1;
                                    links[i].target.flag = 1;
                                    links[i].source.greyflag = 0;
                                    links[i].target.greyflag = 0;
                                    links[i].flag = 2;
                                    jQuery("#linkId_" + links[i].id).css('stroke', 'black');
                                    jQuery("#linkId_" + links[i].id).attr("stroke-dasharray", "0,0");
                                }
                            }
                            for (var i = 0; i < nodes.length; i++) {
                                if (selected_link.source.id == nodes[i].id || selected_link.target.id == nodes[i].id) {
                                    nodes[i].greyflag = 0;
                                }
                            }

                            // forLinks = 1;
                            removeTempNodes();
                            var singleNode = 0;
                            if (nodes.length == 1) {
                                singleNode = 1;
                            }
                            restart();
                            refreshSelectedNode = 0;
                            addSuggestions(singleNode, 1);
                        }
                    }
                });
                // else{
                //   jQuery("#linkDel").text("Add").click(function(){
                //     for(i=0;i<links.length;i++){
                //   if(selected_link.id == links[i].id){
                //     links[i].source.flag = 1;
                //     links[i].target.flag = 1;
                //     links[i].source.greyflag = 0;
                //     links[i].target.greyflag = 0;
                //   }
                // }
                // for(var i = 0; i < nodes.length; i++)
                //    {
                //      if(selected_link.source.id == nodes[i].id || selected_link.target.id == nodes[i].id)
                //   {  nodes[i].greyflag = 0; }
                //   }
                // //   restart();
                // removeTempNodes();
                //        var singleNode = 0;
                //   if (nodes.length == 1){
                //       singleNode = 1;
                //   }
                //   restart();
                //   refreshSelectedNode = 0;
                //        addSuggestions(singleNode, 1);
                //     });
                // }
                jQuery("#linkDel").css("max-width", "70px");
                jQuery("#linkDel").on("mouseover", function() {
                        jQuery("#linkDel").css("background-color", "#d6d6d6");
                    })
                    .on("mouseout", function() {
                        jQuery("#linkDel").css("background-color", "#efefef");
                    });
                jQuery("#button-container").on("mouseover", function() {
                        tooltipEdgeExample.style("visibility", "visible");
                    })
                    .on("mouseout", function() {
                        tooltipEdgeExample.style("visibility", "hidden");
                    });
                var src = document.getElementById('sourceType');
                var tar = document.getElementById('targetType');
                var srcEx = document.getElementById('sourceTypeEx');
                var tarEx = document.getElementById('targetTypeEx');

                if (responseEdge.sourceType == null) {
                    src.innerText = '(' + typeSource.replace(/_/g, " ") + ')';
                } else {
                    src.innerText = '(' + typeSource.toUpperCase().replace(/_/g, " ") + ')';
                }
                if (responseEdge.objectType == null) {
                    tar.innerText = '(' + typeObject.replace(/_/g, " ") + ')';
                } else {
                    tar.innerText = '(' + typeObject.toUpperCase().replace(/_/g, " ") + ')';
                }
                if (responseExample.length == 3) {
                    srcEx.innerText = '(' + responseExample[0].replace(/_/g, " ") + ')';
                    tarEx.innerText = '(' + responseExample[2].replace(/_/g, " ") + ')';
                }




                //             if (subSuggestionsInProgressFlag != true){
                //                 var i = 0;
                //                 var found = 0;
                //                 if (addEdgeButtonEdgeAdded == true){
                //                      mouseOverEdgeFlag = false;
                //                 }
                //                 else{
                //                     if((d.source.greyflag == 2 && d.source.flag == 1) || (d.target.greyflag == 2 && d.target.flag == 1)){
                //                         mouseOverEdgeFlag = false;
                //                     }
                //                     else
                //                     {
                //                         for (i = 0; i < allReturnObject.length; i++)
                //                         {
                //                             var entry = allReturnObject[i];
                //                             if (d.linkID == entry.edge.split('|')[0] && (d.source.greyflag != 0 || d.target.greyflag != 0))
                //                             {
                //                                 found++;
                //                                 if (found > 1){
                //                                      mouseOverEdgeFlag = true;
                //                                      jQuery("#linkId_"+d.id).css('stroke','red');
                //                                      break;
                //                                };
                //                             }
                //                         }
                //                     }
                //                 }
                //             }
                mouseOverEdgeFlag = true;
                selected_link = d;
            }
        });

    //         jQuery("#linkDel").click(function(){
    //   if(selected_link.source.greyflag==0 && selected_link.target.greyflag==0 && selected_link.flag != 20){
    // //     jQuery("#linkDel").text("Delete");
    // for(i=0;i<links.length;i++){
    //   if(selected_link.id == links[i].id){
    //     links[i].source = links[i].target;
    //   }
    // }

    // removeTempNodes();
    //   }
    // else{
    // //    jQuery("#linkDel").text("Add");
    //   for(i=0;i<links.length;i++){
    //   if(selected_link.id == links[i].id){
    //     links[i].source.flag = 1;
    //     links[i].target.flag = 1;
    //     links[i].source.greyflag = 0;
    //     links[i].target.greyflag = 0;
    //   }
    // }
    // for(var i = 0; i < nodes.length; i++)
    //    {
    //      if(selected_link.source.id == nodes[i].id || selected_link.target.id == nodes[i].id)
    //   {  nodes[i].greyflag = 0; }
    //   }

    // removeTempNodes();
    //        var singleNode = 0;
    //   if (nodes.length == 1){
    //       singleNode = 1;
    //   }
    //   restart();
    //   refreshSelectedNode = 0;
    //        addSuggestions(singleNode, 1);
    //         }
    // });

    // jQuery("#linkDel").click(function(){
    // if(selected_link.source.greyflag == 0 && selected_link.target.greyflag == 0){
    // for(i=0;i<links.length;i++){
    //   if(selected_link.id == links[i].id){
    //     links[i].source = links[i].target;
    //   }
    // }
    // restart();
    // }else{
    // for(i=0;i<links.length;i++){
    //   if(selected_link.id == links[i].id){
    //     links[i].source.flag = 1;
    //     links[i].target.flag = 1;
    //     links[i].source.greyflag = 0;
    //     links[i].target.greyflag = 0;
    //   }
    // }
    // for(var i = 0; i < nodes.length; i++)
    //    {
    //      if(selected_link.source.id == nodes[i].id || selected_link.target.id == nodes[i].id)
    //   {  nodes[i].greyflag = 0; }
    //   }
    //   restart();
    // }
    // })

    // NB: the function arg is crucial here! nodes are known by id, not by index!
    circle = circle.data(nodes, function(d) {
        return d.id;
    });

    // update existing nodes (reflexive & selected visual states)
    circle.selectAll('circle')
        // .style('fill', function(d) { return (d === selected_node) ? d.rgb(colors(d.id)).brighter().toString() : colors(d.id); })
        .style('fill', function(d) {
            return ((d != null && d.name.search("&") != -1 && d == selected_node) ? defaultNodeColor : (d.name.search("&") != -1) ? "#ff6600" : (d.flag == 10 && d.greyflag == 10) ? "#96FEFB" : (d.flag == 9 && d.greyflag == 9) ? "#D0D0D0" : (exampleReverseRoleFlag == true && d === exampleTarget) ? "#5bfe29" : (exampleReverseRoleFlag == true && d === exampleSource) ? "#FF6699" : (d.flag == 1 && d == selected_node && d.greyflag == 0) ? d3.rgb(defaultNodeColor) : (d.flag == 1 && d.greyflag == 2) ? "#efefef" : (d.flag == 2 && d.greyflag == 1) ? "#EFEFEF" : d3.rgb(defaultNodeColor))

            // return (d == selected_node && d.flag == 1) ? "None": (d.flag == 2)? "#EFEFEF": "None";
        })
        // .style('stroke', function(d){ if (d.flag == 10 && d.greyflag == 10) {return "#96FEFB"} else if (d.flag == 9 && d.greyflag == 9) {return "#D0D0D0"} else {return 'black'}
        // })
        .style('stroke', function(d) {
            return ((d.flag == 10 && d.greyflag == 10) ? "#74b3b1" : (d.flag == 9 && d.greyflag == 9) ? "#a3a2a2" : (exampleReverseRoleFlag == true && d === exampleTarget) ? "#42ab22" : (exampleReverseRoleFlag == true && d === exampleSource) ? "#d60b18" : (d.flag == 1 && d == selected_node && d.greyflag == 0) ? "#9c4206" : (d.flag == 1 && d.greyflag == 2) ? "#969292" : (d.flag == 2 && d.greyflag == 1) ? "#969292" : d3.rgb("#9c4206"))
        })
        .classed('reflexive', function(d) {
            return d.reflexive;
        });


    circle.selectAll('text')
        .style("fill", function(d) {
            if ((d.flag == 9 && d.greyflag == 9) || (d.flag == 10 && d.greyflag == 10)) {
                return "#AAAAAA"
            } else {
                return "#000000"
            }
        })
        .style("font-family", "Segoe UI")
        .style("font-weight", function(d) {
            if (d.greyflag == 0) {
                return "bold";
            } else
                return "normal";
        })
        .style("font-size", function(d) {
            // var validCondition = false;
            // var count = 0;
            // var previousLinkType;
            // for (var v = 0; v < links.length; v++) {
            //     if (links[v].source === d && links[v].target.greyflag == 0 && links[v].source.greyflag == 0 && links[v].source != links[v].target && links[v].flag == 2 && links[v].source.entity == -1) {
            //         count++;
            //         if (count != 1) {
            //             if (links[v].actualSourceType == previousLinkType) {
            //                 count--;
            //             }
            //         }
            //         previousLinkType = links[v].actualSourceType;
            //     } else if (links[v].target === d && links[v].target.greyflag == 0 && links[v].source.greyflag == 0 && links[v].source != links[v].target && links[v].flag == 2 && links[v].target.entity == -1) {
            //         count++;
            //         if (count != 1) {
            //             if (links[v].actualTargetType == previousLinkType) {
            //                 count--;
            //             }
            //         }
            //         previousLinkType = links[v].actualTargetType;
            //     }
            //     if (count >= 2) {
            //         validCondition = true;
            //         break;
            //     }
            // }
            // if (validCondition == true) {
                return "15px";
            // } else {
            //     return "15px";
            // }
        })
        .text(function(d) {
            var validCondition = false;
            var count = 0;
            var previousLinkType;
            for (var v = 0; v < links.length; v++) {
                if (links[v].source === d && links[v].target.greyflag == 0 && links[v].source.greyflag == 0 && links[v].source != links[v].target && links[v].flag == 2 && links[v].source.entity == -1) {
                    count++;
                    if (count != 1) {
                        if (links[v].actualSourceType == previousLinkType) {
                            count--;
                        }
                    }
                    previousLinkType = links[v].actualSourceType;
                } else if (links[v].target === d && links[v].target.greyflag == 0 && links[v].source.greyflag == 0 && links[v].source != links[v].target && links[v].flag == 2 && links[v].target.entity == -1) {
                    count++;
                    if (count != 1) {
                        if (links[v].actualTargetType == previousLinkType) {
                            count--;
                        }
                    }
                    previousLinkType = links[v].actualTargetType;
                }
                if (count >= 1) {
                    validCondition = true;
                    break;
                }
            }

            if (validCondition == true) {
                var tooltipString = d.tempName;
                if (d.greyflag == 0 && d.flag == 1) {
                    var resultData = [];
                    var nodeMouseOver = d;
                    var nodeEdgesMouseOver;
                    var count = 0;
                    var totalCount = 0;
                    for (var k = 0; k < links.length; k++) {
                        if (links[k].source != links[k].target && links[k].source === nodeMouseOver && links[k].target.greyflag == 0 && links[k].target.flag == 1 && links[k].flag == 2 && nodeMouseOver.entity == -1) {

                            if (count == 0) {

                                count++;
                                totalCount++;
                                nodeEdgesMouseOver = links[k].linkID + "," + "0";


                            } else {

                                nodeEdgesMouseOver = nodeEdgesMouseOver + "|" + links[k].linkID + "," + "0";

                                totalCount++;

                            }
                        } else if (links[k].source != links[k].target && links[k].target === nodeMouseOver && links[k].source.greyflag == 0 && links[k].source.flag == 1 && links[k].flag == 2 && nodeMouseOver.entity == -1) {
                            if (count == 0) {

                                count++;
                                nodeEdgesMouseOver = links[k].linkID + "," + "1";
                                totalCount++;

                            } else {

                                nodeEdgesMouseOver = nodeEdgesMouseOver + "|" + links[k].linkID + "," + "1";
                                totalCount++;

                            }
                        }

                    }
                    if (totalCount > 0) {

                        setMouseOverFlagOfIndex();
                        resultData.length = 0;
                        getRequest("https://" + urlFull + "/orion_app/getnodetypevalues?node=" + nodeMouseOver.nodeID + "&edges=" + nodeEdgesMouseOver, resultData);
                        reSetMouseOverFlagOfIndex();
                        if (resultData.length > 0) {
                            mouseOverFlag = true;
                            //                       tooltip.style("visibility", "visible");
                            //                       var tooltipString;
                            for (var t = 0; t < resultData.length; t++) {
                              if(tooltipString.toLowerCase().split(' & ').contains(resultData[t][1]) == false
                            && tooltipString.toLowerCase().split(' & ').contains(resultData[t][1].replace(/_/g, " ")) == false) {
                                if(tooltipString == "") {
                                  tooltipString = resultData[t][1];
                                } else {
                                  tooltipString = tooltipString + " & " + resultData[t][1];
                                }
                              }
                            }
                        }
                    }
                }

                d.name = tooltipString.replace(/\w\S*/g, function(txt) {
                    return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
                });
            } else {
                d.name = d.tempName.replace(/_/g, " ");
            }
            if (d.entity == -1) {
                d.name = (d.name).toUpperCase();
            }
            return d.name.replace(/_/g, " ");
        });

    // add new nodes
    var g1 = circle
        //            .on("mousemove", function(){
        //                     if (mouseOverFlag == true){
        //                     return tooltip.style("top", (event.pageY-20)+"px").style("left",(event.pageX+20)+"px");}
        //            })
        //      .on("mouseout", function(){
        //                     if (mouseOverFlag == true){
        //                        mouseOverFlag = false;
        //                        return tooltip.style("visibility", "hidden");}
        //            })
        .on('mouseover', function(d) {
            this.style.cursor = 'pointer';
            if (d.greyflag == 0 && d.flag == 1) {
                var resultData = [];
                var nodeMouseOver = d;
                var nodeEdgesMouseOver;
                var count = 0;
                var totalCount = 0;
                for (var k = 0; k < links.length; k++) {
                    if (links[k].source != links[k].target && links[k].source === nodeMouseOver && links[k].target.greyflag == 0 && links[k].target.flag == 1 && links[k].flag == 2 && nodeMouseOver.entity == -1) {

                        if (count == 0) {
                            count++;
                            totalCount++;
                            nodeEdgesMouseOver = links[k].linkID + "," + "0";
                        } else {
                            nodeEdgesMouseOver = nodeEdgesMouseOver + "|" + links[k].linkID + "," + "0";
                            totalCount++;
                        }
                    } else if (links[k].source != links[k].target && links[k].target === nodeMouseOver && links[k].source.greyflag == 0 && links[k].source.flag == 1 && links[k].flag == 2 && nodeMouseOver.entity == -1) {
                        if (count == 0) {
                            count++;
                            nodeEdgesMouseOver = links[k].linkID + "," + "1";
                            totalCount++;
                        } else {
                            nodeEdgesMouseOver = nodeEdgesMouseOver + "|" + links[k].linkID + "," + "1";
                            totalCount++;
                        }
                    }
                }
                if (totalCount > 1) {
                    setMouseOverFlagOfIndex();
                    resultData.length = 0;
                    getRequest("https://" + urlFull + "/orion_app/getnodetypevalues?node=" + nodeMouseOver.nodeID + "&edges=" + nodeEdgesMouseOver, resultData);
                    reSetMouseOverFlagOfIndex();
                    if (resultData.length > 1) {
                        mouseOverFlag = true;
                        //                       tooltip.style("visibility", "visible");
                        var tooltipString;
                        for (var t = 0; t < resultData.length; t++) {
                            if (t == 0) {
                                tooltipString = resultData[t][1].replace(/_/g, " ");
                            } else {
                                tooltipString = tooltipString + ", " + resultData[t][1].replace(/_/g, " ");
                            }
                        }
                        //                       tooltip.text(tooltipString.replace(/\w\S*/g, function(txt){return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();}));
                    }
                }
            }
        }).enter().append('svg:g').attr('class', 'conceptG')
        .call(drag);

    g1.append('svg:circle')
        // .attr('class', 'node')
        .attr('r', 30)
        .style('fill', function(d) {
            // return (d == selected_node && d.flag == 1) ? d3.rgb(defaultNodeColor).brighter(): (d.flag == 2)? "#EFEFEF": d3.rgb(defaultNodeColor)
            // return (d == selected_node && d.flag == 1) ? d3.rgb(defaultNodeColor).brighter(): (d.flag == 2)? "#EFEFEF": "None";
            return ((d.flag == 10 && d.greyflag == 10) ? "#96FEFB" : (d.flag == 9 && d.greyflag == 9) ? "#D0D0D0" : (exampleReverseRoleFlag == true && d === exampleTarget) ? "#5bfe29" : (exampleReverseRoleFlag == true && d === exampleSource) ? "#FF6699" : (d == selected_node && d.flag == 1 && d.greyflag == 0) ? "#33CCFF" : (d.flag == 1 && d.greyflag == 2) ? "#2EFEF7" : (d.flag == 2 && d.greyflag == 1) ? "#EFEFEF" : d3.rgb(defaultNodeColor))
        })
        .style('stroke', function(d) {
            if (d.flag == 10 && d.greyflag == 10) {
                return "#96FEFB"
            } else if (d.flag == 9 && d.greyflag == 9) {
                return "#D0D0D0"
            } else {
                return 'black'
            }
        })
        .classed('reflexive', function(d) {
            return d.reflexive;
        })
        .on('mouseout', function(d) {
            setTimeout(function() {
                if (!mouseOnOption) {
                    //         force.start();
                    force.alpha(alpha);
                }
            }, 300);

            delNode.style("visibility", 'hidden');
            editNode.style("visibility", 'hidden');
            suggNode.style("visibility", 'hidden');
            if (!mousedown_node || d === mousedown_node) return;
            // unenlarge target node
            d3.select(this).attr('transform', '');

        })
        //     .on('dblclick', function(d){
        //        //Changed By Heet
        //        removeTempNodes();
        //      restart();
        //          var dblClickValid = true;
        //          if (exampleReverseRoleFlag == true){
        //               dblClickValid = false;
        //          }
        //          else {
        //              for(var x = 0; x < links.length; x++){
        //                  if (links[x].source != links[x].target && ((links[x].source.greyflag != 0 || links[x].target.greyflag != 0) || (links[x].flag == 3 || links[x].flag == 4))){
        //                      dblClickValid = false;
        //                      break;
        //                  }
        //              }
        //          }
        //          if (dblClickValid == true){
        //              nodeToBeEdited = d;
        //              editNodeKeyword = "";
        //              nodeEditList.length = 0;
        //              nodeEditEdges.length = 0;
        //              nodeEditWindowNo = 0;
        //              var count = 0;
        //              for(var k = 0; k < links.length; k++){
        //                  if (links[k].source != links[k].target && links[k].source === nodeToBeEdited){
        //                       if (count == 0){
        //                            count++;
        //                            nodeEditEdges = links[k].linkID+","+"0";
        //                       }
        //                       else {
        //                            nodeEditEdges = nodeEditEdges+"|"+links[k].linkID +","+"0";
        //                       }
        //                  }
        //                  else if (links[k].source != links[k].target && links[k].target === nodeToBeEdited){
        //                       if (count == 0){
        //                            count++;
        //                            nodeEditEdges = links[k].linkID+","+"1";
        //                       }
        //                       else {
        //                            nodeEditEdges = nodeEditEdges+"|"+links[k].linkID +","+"1";
        //                       }
        //                  }

        //              }
        //              getRequestWithLoader("https://" + urlFull + "/orion_app/geteditnode?node="+nodeToBeEdited.nodeID+"&windownum="+0+"&windowsize="+windowSize+"&edges="+nodeEditEdges+"&keyword="+editNodeKeyword);
        //          }
        //     })
        .on('mousedown', function(d) {
            //       delNode.style("visibility",'hidden');
            //       editNode.style("visibility",'hidden');
            //       suggNode.style("visibility",'hidden');
            nodeclick = true;
            allowNodeCreation = false;
            // select node
            mousedown_node = d;

            //           if(drawEdge == false){
            //             checkMove_x = d.x;
            //             checkMove_y = d.y;
            //             if(mousedown_node === selected_node) {
            //                 selected_node = null;
            //                 suggestion_mode = 0;
            //     if(mousedown_node.greyflag == 2 && mousedown_node.flag == 1){
            //                    d.greyflag = 1;
            //                    d.flag = 2;
            //                    noOfSuggestionsPicked--;
            //                    if (subSuggestionsInProgressFlag == true){
            //                        SugPickdPerGreyLinkCount--;
            //                        animatedHelp();
            //                        }
            //                    if (noOfSuggestionsPicked == 0) { suggestionPicked = false;
            //                        if (greyLinkSuggestionsPicked == 0 && subSuggestionsInProgressFlag == false){
            //                            jQuery("#button-g").show();}
            //                    }
            //                 }
            //     console.log("node not selected")
            //                }
            //       else if (mousedown_node.flag == 1 && mousedown_node.greyflag == 2){
            //              selected_node = null; suggestion_mode=0; d.greyflag = 1; d.flag = 2; console.log("suggestion deselected");
            //                    noOfSuggestionsPicked--;
            //                    if (subSuggestionsInProgressFlag == true){
            //                        SugPickdPerGreyLinkCount--;
            //                        animatedHelp();
            //                        }
            //                    if (noOfSuggestionsPicked == 0) { suggestionPicked = false;
            //                          if(greyLinkSuggestionsPicked == 0 && subSuggestionsInProgressFlag == false){   jQuery("#button-g").show();}
            //                    }
            //             }
            // //             else {
            selected_node = d;
            //               console.log("node selected");
            //               suggestion_mode = selected_node.id;
            //             }
            //           }

            // reposition drag line
            if (drawEdge) {
                //               var unconnected = null;
                delNode.style("visibility", 'hidden');
                editNode.style("visibility", 'hidden');
                suggNode.style("visibility", 'hidden');
                nodeclick = true;
                selected_node = d;
                console.log("node selected");
                suggestion_mode = selected_node.id;
                console.log(drawEdge);
                hideDragLineFlag = false;
                /* Edited by AD */
                //               for (var y=0;y <nodes.length; y++){
                //                       var unc_flag = false;
                //                       for(var z = 0; z < links.length; z++){
                //                         if(links[z].source != links[z].target && (links[z].source.id == nodes[y].id || links[z].target.id == nodes[y].id) ){
                //                           unc_flag=true;
                //                         }

                //                     }
                //                     if(!unc_flag){
                //                           unconnected = nodes[y].id;
                //                           break;
                //                         }
                //                   }
                /* Edited by AD */
                //               for(var x = 0; x < links.length; x++){
                //                   if (links[x].source != links[x].target && ((links[x].source.greyflag != 0 || links[x].target.greyflag != 0) || (links[x].flag == 3 || links[x].flag == 4))){
                //                        hideDragLineFlag = true;
                //                        break;
                //                   }

                /* Edited by AD */
                //                   if(unconnected != null && links[x].source != links[x].target && (links[x].source.id==mousedown_node.id || links[x].target.id==mousedown_node.id)){
                //                     hideDragLineFlag = true;
                //                     break;

                //                   }
                /* Edited by AD */
                //               }
                //               if (hideDragLineFlag == true){
                //                    console.log("Edge can not be drawn with graph containing grey nodes");
                //               }
                //               else{
                drag_line
                    .style('marker-end', 'url(#end-arrow)')
                    .classed('hidden', false)
                    //.attr('d', 'M' + mousedown_node.x + ',' + mousedown_node.y + 'L' + mousedown_node.x + ',' + mousedown_node.y);
                    .attr('d', 'M' + d.x + ',' + d.y + 'A' + 0 + ',' + 0 + ' 0 0,1 ' + d.x + ',' + d.y);

                //.attr('d', 'M' + mousedown_node.x + ',' + mousedown_node.y + 'A' + mousedown_node.x  + ',' + mousedown_node.y + ' 0 0,1 ' + mousedown_node.x  + ',' + mousedown_node.y);
                //               }
            };
        })

        .on('mouseover', function(d) {
            alpha = force.alpha();
            force.stop();
            if (!translateAllowed) {

                if (d.greyflag != 1) {
                    jQuery("#delAdd").text("delete");
                    editNode.style("visibility", 'visible');
                    editNode.style("top", (d.y + 100) + "px").style("left", (d.x + 25) + "px");
                    if(systemName === "naive") suggNode.style("visibility", 'hidden');
                    else {
                        suggNode.style("visibility", 'visible');
                        suggNode.style("top", (d.y + 100) + "px").style("left", (d.x - 59) + "px");
                    }
                    
                } else {
                    jQuery("#delAdd").text("add");
                }
                delNode.style("visibility", 'visible');

                delNode.style("top", (d.y + 80) + "px").style("left", (d.x - 18) + "px"); //Edited by AD
                //         editNode.style("visibility",'visible');
                //         editNode.style("top", (d.y+50)+"px").style("left",(d.x+38)+"px");
                //         suggNode.style("visibility",'visible');
                //         suggNode.style("top", (d.y+50)+"px").style("left",(d.x-46)+"px");
                selected_node = d;
                suggestion_mode = selected_node.id;
                //         .style("top", (event.pageY-20)+"px").style("left",(event.pageX+20)+"px");

            }

        })
        .on('mouseup', function(d) {
            //       delNode.style("visibility",'visible');
            //       editNode.style("visibility",'visible');
            //       suggNode.style("visibility",'visible');
            //  if (drawEdge == false)
            //  {
            //       if (mousedown_node.x != checkMove_x || mousedown_node.y != checkMove_y)
            // {
            nodeclick = false;
            allowNodeCreation = true;
            //             if (mousedown_node.greyflag == 1 && mousedown_node.flag == 2 && selected_node == null){
            //            d.greyflag = 2;
            //                  d.flag = 1;
            //                  noOfSuggestionsPicked++;
            //                        if (noOfSuggestionsPicked == 0 || noOfSuggestionsPicked == 1){
            //                                displayFlag = 1;
            //                        }
            //            if (subSuggestionsInProgressFlag == true){
            //               SugPickdPerGreyLinkCount++;
            //                             if (SugPickdPerGreyLinkCount == 1 && greyLinkPickdPerSubSuggestion == 0 && noOfSuggestionsPicked > 1){
            //                                displayFlag = 1;
            //                             }
            //                          // animatedHelp();
            //                           }
            //            suggestionPicked = true;
            //            jQuery("#button-g").hide();
            //            selected_node = d;
            //              }
            //              else if ( selected_node === mousedown_node){
            //                  selected_node = null;
            //              }
            //      }
            //      checkMove_x = 0;
            //      checkMove_y = 0;
            // }
            //           if (selected_node != null && mousedown_node == d && d.flag == 2 && d.greyflag == 1 && drawEdge == false) {
            //                d.flag=1;
            //                noOfSuggestionsPicked++;
            //                if (noOfSuggestionsPicked == 0 || noOfSuggestionsPicked == 1){
            //                   displayFlag = 1;
            //                }
            //                if (subSuggestionsInProgressFlag == true){
            //                    SugPickdPerGreyLinkCount++; //animatedHelp();
            //                             if (SugPickdPerGreyLinkCount == 1 && greyLinkPickdPerSubSuggestion == 0 && noOfSuggestionsPicked > 1){
            //                                displayFlag = 1;
            //                             }
            //                }
            //                suggestionPicked = true;
            //                d.greyflag = 2;
            //                jQuery("#button-g").hide();
            //           };
            //           if (selected_node == d) {
            //                    jQuery('#myModal').modal('show');
            //                    jQuery('#selectDiv').hide();
            //                    jQuery("#modal-text").empty();
            //                    jQuery("#modal-text").append("<h4>Node Values</h4>"+
            //                                              "Domain: "+selected_node.domain+"<br>"+
            //                                              "Type: "+selected_node.type+"<br>");
            //           };
            mouseup_node = d;
            // check for drag-to-self
            if (drawEdge) {
                //                    if (hideDragLineFlag == true) { hideDragLineFlag = false; resetMouseVars(); return;}
                if (mouseup_node === mousedown_node) {
                    resetMouseVars();
                    return;
                }

                if (exampleReverseRoleFlag == true) {
                    jQuery("#linkId_" + exampleLinkGraphId).css('stroke', 'black');
                    exampleSource = null;
                    exampleTarget = null;
                    exampleLinkId = -1;
                    exampleLinkGraphId = -1;
                    exampleReverseRoleFlag = false;
                    animatedHelp();
                    exampleActualSourceType = null;
                    exampleActualTargetType = null;
                    addEdgeButtonEdgeAdded = false;
                    jQuery("#Add-Edge-button").hide();
                    jQuery("#example-text").show();
                    jQuery("#active-examples").empty();
                    jQuery("#Reverse-button").hide();
                }
                // unenlarge target node
                //  d3.select(this).attr('transform', '');

                // add link to graph (update if exists)
                // NB: links are strictly source < target; arrows separately specified by booleans
                var source, target, direction;

                source = mousedown_node;
                target = mouseup_node;

                var c = 0;
                for (var i = 0; i < links.length; i++) {
                    if ((links[i].source.id == source.id && links[i].target.id == target.id && links[i].flag == 2) || (links[i].source.id == target.id && links[i].target.id == source.id && links[i].flag == 2)) {
                        c += 1;
                    }
                }
                if (c < 2) {
                    ++lastEdgeID;
                    var actualSourceType;
                    var actualTargetType;
                    if (source.entity == -1) {
                        actualSourceType = source.nodeID;
                    } else {
                        actualSourceType = source.type;
                    }
                    if (target.entity == -1) {
                        actualTargetType = target.nodeID;
                    } else {
                        actualTargetType = target.type;
                    }

                    link = {
                        source: source,
                        target: target,
                        value: "",
                        id: lastEdgeID,
                        linkNum: 1,
                        linkID: -1,
                        flag: 2,
                        actualSourceType: actualSourceType,
                        actualTargetType: actualTargetType
                    };
                    for (var i = 0; i < links.length; i++) {
                        if (links[i].source.nodeID == link.source.nodeID && links[i].target.nodeID == link.target.nodeID && links[i].source != links[i].target)
                            link.linkNum++;
                    }
                    selected_node = null;
                    if (source != null && target != null && lastEdgeID != null) {
                        links.push(link);
                        if (selected_link) {
                            if (selected_link.flag != 2) {
                                if (selected_link.flag == 3) {
                                    jQuery("#linkId_" + selected_link.id).css('stroke', '#B0B0B0');
                                } else if (selected_link.flag == 4) {
                                    jQuery("#linkId_" + selected_link.id).css('stroke', '#2EFEF7');
                                }
                            } else {
                                jQuery("#linkId_" + selected_link.id).css('stroke', 'black');
                            }
                        }
                        selected_link = link;
                        jQuery("#type-div").hide();
                        jQuery("#entity-div").hide();
                        jQuery("#domain-div").hide();
                        jQuery("#entity-border").hide();
                        jQuery("#type-border").hide();
                        jQuery("#selected-div").hide();
                        jQuery("#type-keyword-div").hide();
                        /* Edited by AD */
                        var unconnected = null;
                        // for (var y = 0; y < nodes.length; y++) {
                        //     var unc_flag = false;
                        //     for (var z = 0; z < links.length; z++) {
                        //         if (links[z].source != links[z].target && (links[z].source.id == nodes[y].id || links[z].target.id == nodes[y].id)) {
                        //             unc_flag = true;
                        //         }
                        //
                        //     }
                        //     if (!unc_flag) {
                        //         unconnected = nodes[y].id;
                        //         break;
                        //     }
                        // }



                        if (unconnected != null) {
                            if (unconnected == source.id || unconnected == target.id) {
                                jQuery("#edge-div").show();
                                jQuery("#keyword-div").hide();
                                link_click = true;
                                allowNodeCreation = true;
                                nodeclick = false;
                                partialGraph.length = 0;
                                for (var i = 0, j = 0; i < links.length; i++) {
                                    if (links[i].linkID != -1 && (links[i].source != links[i].target) && (links[i].source.greyflag == 0 || links[i].target.greyflag == 0)) {
                                        partialGraph[j] = {
                                            source: links[i].source.nodeID,
                                            graphSource: links[i].source.id,
                                            edge: links[i].linkID,
                                            object: links[i].target.nodeID,
                                            graphObject: links[i].target.id,
                                            sourceTypeValues: getTypeValues(links[i].source.typeList)+","+getTypeValues(findEdgeEndTypes(links[i].source)),
                                            objectTypeValues: getTypeValues(links[i].target.typeList)+","+getTypeValues(findEdgeEndTypes(links[i].target)),
                                            sourceEntity: links[i].source.entity,
                                            objectEntity: links[i].target.entity
                                        };
                                        j++;
                                    }
                                };

                                for (var l = 0, k = partialGraph.length; l < nodes.length; l++) {
                                    var checklink = false;
                                    for (var i = 0; i < links.length; i++) {
                                        if ((nodes[l] == links[i].source || nodes[l] == links[i].target) && links[i].source != links[i].target) {
                                            checklink = true;
                                        }
                                    }
                                    if (!checklink) {
                                        partialGraph[k] = {
                                            source: nodes[l].nodeID,
                                            graphSource: nodes[l].id,
                                            edge: -1,
                                            object: 0,
                                            graphObject: -1,
                                            sourceTypeValues: getTypeValues(nodes[l].typeList)+","+getTypeValues(findEdgeEndTypes(nodes[l])),
                                            objectTypeValues: "",
                                            sourceEntity: nodes[l].entity,
                                            objectEntity: -1
                                        };
                                        k++;
                                    }
                                }
                                var data = {
                                    partialGraph: partialGraph,
                                    nodes: nodes,
                                    mode: 0,
                                    rejectedGraph: rejectedGraph,
                                    activeEdgeEnds: {
                                        source: selected_link.source.nodeID,
                                        object: selected_link.target.nodeID,
                                        graphSource: selected_link.source.id,
                                        graphObject: selected_link.target.id
                                    },
                                    dataGraphInUse: 0,
                                    topk: noOfSuggestions,
                                    refreshGraphNode: 0
                                };
                                var edgeMode2 = 4;

                                //                            postRequest(data,edgeMode2);
                            } else {
                                selected_link.source = selected_link.target;
                                selected_link = null;
                                //     allowNodeDrag = true;
                                //     translateAllowed = true;
                                //     drawEdge = false;
                                allowNodeCreation = false;
                                checkKeywordStatus();
                                restart();
                            }
                        } else {
                            jQuery("#edge-div").show();
                            jQuery("#keyword-div").hide();
                            link_click = true;
                            allowNodeCreation = true;
                            nodeclick = false;
                            partialGraph.length = 0;
                            // for (var i = 0, j = 0; i < links.length; i++) {
                            //     if (links[i].linkID != -1 && (links[i].source != links[i].target) && (links[i].source.greyflag == 0 || links[i].target.greyflag == 0)) {
                            //         partialGraph[j] = {
                            //             source: links[i].source.nodeID,
                            //             graphSource: links[i].source.id,
                            //             edge: links[i].linkID,
                            //             object: links[i].target.nodeID,
                            //             graphObject: links[i].target.id,
                            //             sourceTypeValues: getTypeValues(links[i].source.typeList)+","+getTypeValues(findEdgeEndTypes(links[i].source)),
                            //             objectTypeValues: getTypeValues(links[i].target.typeList)+","+getTypeValues(findEdgeEndTypes(links[i].target)),
                            //             sourceEntity: links[i].source.entity,
                            //             objectEntity: links[i].target.entity
                            //         };
                            //         j++;
                            //     }
                            // };
                            // for (var l = 0, k = partialGraph.length; l < nodes.length; l++) {
                            //     var checklink = false;
                            //     for (var i = 0; i < links.length; i++) {
                            //         if (links[i].linkID != -1 && (nodes[l] == links[i].source || nodes[l] == links[i].target) && links[i].source != links[i].target) {
                            //             checklink = true;
                            //         }
                            //     }
                            //     if (!checklink) {
                            //         partialGraph[k] = {
                            //             source: nodes[l].nodeID,
                            //             graphSource: nodes[l].id,
                            //             edge: -1,
                            //             object: 0,
                            //             graphObject: -1,
                            //             sourceTypeValues: getTypeValues(nodes[l].typeList)+","+getTypeValues(findEdgeEndTypes(nodes[l])),
                            //             objectTypeValues: "",
                            //             sourceEntity: nodes[l].entity,
                            //             objectEntity: -1
                            //         };
                            //         k++;
                            //     }
                            // }
                            for (var i = 0, j = 0; i < links.length; i++) {
                                if (links[i].linkID != -1 && (links[i].source != links[i].target) && (links[i].source.greyflag == 0 && links[i].target.greyflag == 0)) {
                                    partialGraph[j] = {
                                        source: links[i].source.nodeID,
                                        graphSource: links[i].source.id,
                                        edge: links[i].linkID,
                                        object: links[i].target.nodeID,
                                        graphObject: links[i].target.id,
                                        sourceTypeValues: getTypeValues(links[i].source.typeList)+","+getTypeValues(findEdgeEndTypes(links[i].source)),
                                        objectTypeValues: getTypeValues(links[i].target.typeList)+","+getTypeValues(findEdgeEndTypes(links[i].target)),
                                        sourceEntity: links[i].source.entity,
                                        objectEntity: links[i].target.entity
                                    };
                                    j++;
                                }
                            };
                            for (var l = 0, k = partialGraph.length; l < nodes.length; l++) {
                              if(nodes[l].greyflag != 0) continue;
                                var checklink = false;
                                for (var i = 0; i < links.length; i++) {
                                    if (links[i].linkID != -1 && (nodes[l] == links[i].source || nodes[l] == links[i].target) && (links[i].source.greyflag == 0 && links[i].target.greyflag == 0) && links[i].source != links[i].target) {
                                        checklink = true;
                                    }
                                }
                                if (!checklink) {
                                    partialGraph[k] = {
                                        source: nodes[l].nodeID,
                                        graphSource: nodes[l].id,
                                        edge: -1,
                                        object: 0,
                                        graphObject: -1,
                                        sourceTypeValues: getTypeValues(nodes[l].typeList)+","+getTypeValues(findEdgeEndTypes(nodes[l])),
                                        objectTypeValues: "",
                                        sourceEntity: nodes[l].entity,
                                        objectEntity: -1
                                    };
                                    k++;
                                }
                            }
                            var data = {
                                partialGraph: partialGraph,
                                nodes: nodes,
                                mode: 0,
                                rejectedGraph: rejectedGraph,
                                activeEdgeEnds: {
                                    source: selected_link.source.nodeID,
                                    object: selected_link.target.nodeID,
                                    graphSource: selected_link.source.id,
                                    graphObject: selected_link.target.id
                                },
                                dataGraphInUse: 0,
                                topk: noOfSuggestions,
                                refreshGraphNode: 0
                            };
                            var edgeMode2 = 4;

                            postRequest(data, edgeMode2);
                        }
                        //                                  jQuery("#edge-div").show();
                        //                                  jQuery("#keyword-div").hide();
                        //                                  link_click=true;
                        /* Edited by AD */
                    } else
                        --lastEdgeID;
                } else {
                    jQuery("#twoEdgeModal").modal('show');
                }

                //                          allowNodeCreation = true;
                //                          nodeclick = false;
                //                          partialGraph.length = 0;
                //                          for (var i = 0, j=0; i < links.length; i++) {
                //                                if (links[i].linkID != -1 && (links[i].source != links[i].target)) {
                //                                     partialGraph[j] = {source: links[i].source.nodeID, graphSource: links[i].source.id,edge: links[i].linkID, object: links[i].target.nodeID, graphObject: links[i].target.id};
                //                                     j++;
                //                                 }
                //                           };

                //                           var data = {
                //                                partialGraph: partialGraph,
                //                                mode: 0,
                //                                rejectedGraph: rejectedGraph,
                //                                activeEdgeEnds: {source: selected_link.source.nodeID, object: selected_link.target.nodeID},
                //                                dataGraphInUse: 0,
                //                                topk: noOfSuggestions,
                //                                refreshGraphNode: 0
                //                            };

                //                            // temp = postRequest(data, linkTypes, returnObject);
                //                            // linkTypes= temp[0];
                //                            // returnObject = temp[1];
                //                            var edgeMode2 = 4;
                //                            postRequest(data,edgeMode2);
            };
        });

    // show node IDs
    g1.append('svg:text')
        .attr("x", "0em")
        .attr("y", ".31em")
        //.style("font-size", "15px")
        .style("text-anchor", "middle")
        .style("fill", function(d) {
            if ((d.flag == 9 && d.greyflag == 9) || (d.flag == 10 && d.greyflag == 10)) {
                return "#AAAAAA"
            } else {
                return "#000000"
            }
        })
        .attr('class', 'id')
        .attr('id', function(d) {
            return 'node' + d.id
        })
        .style("font-size", function(d) {
            var validCondition = false;
            var count = 0;
            var previousLinkType;
            for (var v = 0; v < links.length; v++) {

                if (links[v].source === d && links[v].target.greyflag == 0 && links[v].source.greyflag == 0 && links[v].source != links[v].target && links[v].flag == 2 && links[v].source.entity == -1) {
                    count++;
                    if (count != 1) {
                        if (links[v].actualSourceType == previousLinkType) {
                            count--;
                        }
                    }
                    previousLinkType = links[v].actualSourceType;
                } else if (links[v].target === d && links[v].target.greyflag == 0 && links[v].source.greyflag == 0 && links[v].source != links[v].target && links[v].flag == 2 && links[v].target.entity == -1) {
                    count++;
                    if (count != 1) {
                        if (links[v].actualTargetType == previousLinkType) {
                            count--;
                        }
                    }
                    previousLinkType = links[v].actualTargetType;
                }
                if (count >= 2) {
                    validCondition = true;
                    break;
                }
            }
            if (validCondition == true) {
                return "15px";
            } else {
                return "15px";
            }
        })
        .text(function(d) {
            var validCondition = false;
            var count = 0;
            var previousLinkType;
            for (var v = 0; v < links.length; v++) {
                if (links[v].source === d && links[v].target.greyflag == 0 && links[v].source.greyflag == 0 && links[v].source != links[v].target && links[v].flag == 2 && links[v].source.entity == -1) {
                    count++;
                    if (count != 1) {
                        if (links[v].actualSourceType == previousLinkType) {
                            count--;
                        }
                    }
                    previousLinkType = links[v].actualSourceType;
                } else if (links[v].target === d && links[v].target.greyflag == 0 && links[v].source.greyflag == 0 && links[v].source != links[v].target && links[v].flag == 2 && links[v].target.entity == -1) {
                    count++;
                    if (count != 1) {
                        if (links[v].actualTargetType == previousLinkType) {
                            count--;
                        }
                    }
                    previousLinkType = links[v].actualTargetType;
                }
                if (count >= 2) {
                    validCondition = true;
                    break;
                }
            }
            if (validCondition == true) {
                d.name = "?";
            } else {
                d.name = d.tempName;
            }
            if (d.entity == -1) {
                d.name = (d.name).toUpperCase();
            }
            return d.name.replace(/_/g, " ");
        });


    /* Adds options to Select Type and Select Entity when a node is selected*/
    if (selected_node != null) {
        jQuery("#node" + selected_node.id).empty().append(selected_node.name.replace(/_/g, " "));
    };

    if (selected_link != null) {
        jQuery("#textPath" + selected_link.id).empty().append(selected_link.value.replace(/_/g, " "));
    };

    // remove old nodes
    circle.exit().remove();

    for (var i = 0; i < links.length; i++) {
        if (links[i].flag == 4) {
            jQuery("#linkId_" + links[i].id).css('stroke', '#2EFEF7');
        }
    }
    // set the graph in motion
    force.start();
}



function getExamples(link) {
    exampleReverseRoleFlag = false;
    animatedHelp();
    exampleSource = link.source;
    exampleLinkId = link.linkID;
    exampleLinkValue = link.value;
    exampleLinkGraphId = link.id;
    exampleTarget = link.target;
    exampleActualSourceType = link.actualSourceType;
    exampleActualTargetType = link.actualTargetType;
    var sourceNodeId = link.source.nodeID;
    var targetNodeId = link.target.nodeID;
    var edgeId = link.linkID;
    jQuery("#example-text").hide();
    setExamplesFlag();
    response.length = 0;
    getRequest("https://" + urlFull + "/orion_app/getexamples?edge=" + edgeId + "&source=" + sourceNodeId + "&object=" + targetNodeId, response);
    responseExample = [];
    jQuery.ajax({
        url: "https://xxx.uta.edu/orion_app/getedgepreview?edgeid=" + edgeId,
        type: 'get',
        async: false,
        success: function(r) {
            responseExample = r;
        }
    });
    setExamplesFlag();
    animatedExamples();
    addEdgeButtonEdgeAdded = false;
    jQuery("#Add-Edge-button").show();
    if (response[0].isReversible == true) {
        jQuery("#Reverse-button").show();
    }
    restart();
    if (edgeTypesFlag == 0) {
        CheckOtherWraps();
        edgeTypesFlag = 1;
    } else {
        edgeTypesFlag = 0;
    }
    //jQuery("#wrap4").toggleClass('active');

    jQuery("#edge-types").toggleClass('active');
}


function animatedExamples() {
    jQuery("#active-examples").empty();
    var typeSource;
    var typeObject;
    if (response[0].sourceType == null && response[0].objectType == null) {
        typeSource = exampleSource.name;
        typeObject = exampleTarget.name;
    } else if (response[0].sourceType == null) {
        typeSource = exampleSource.name;
        typeObject = response[0].objectType.split(',')[1];
    } else if (response[0].objectType == null) {
        typeObject = exampleTarget.name;
        typeSource = response[0].sourceType.split(',')[1];
    } else {
        typeSource = response[0].sourceType.split(',')[1];
        typeObject = response[0].objectType.split(',')[1];
    }
    jQuery("#active-examples").append('<li>' + exampleLinkValue + '</li>');
    jQuery("#active-examples").append('<li class="li-animation" style="font-size: 15px; list-style-type:none;padding:0px; margin:0px"><span style="color:#0047ab" id="sourceType"></span> <span style="color:#063145;"> &#8594 </span><span id="targetType" style="color:#00CC00;"></span></li>');
    jQuery("#active-examples").append('<li class="li-animation" style="font-size: 15px; list-style-type:none;padding:0px; margin:0px">Example: <span style="color:#0047ab" id="sourceTypeEx"></span> <span style="color:#063145;"> &#8594 </span><span id="targetTypeEx" style="color:#00CC00;"></span></li>');
    var src = document.getElementById('sourceType');
    var tar = document.getElementById('targetType');
    var srcEx = document.getElementById('sourceTypeEx');
    var tarEx = document.getElementById('targetTypeEx');

    if (response[0].sourceType == null) {
        src.innerText = '(' + typeSource + ')';
    } else {
        src.innerText = '(' + typeSource.toUpperCase() + ')';
    }
    if (response[0].objectType == null) {
        tar.innerText = '(' + typeObject + ')';
    } else {
        tar.innerText = '(' + typeObject.toUpperCase() + ')';
    }
    if (responseExample.length == 3) {
        srcEx.innerText = '(' + responseExample[0] + ')';
        tarEx.innerText = '(' + responseExample[2] + ')';
    }

    var textSource;
    var textTarget;
    for (var i = 0; i < response.length; i++) {
        for (var j = 0; j < response[i].examples.length; j++) {
            var srcid = "source" + j;
            var tarid = "target" + j;
            var exmSource = response[i].examples[j].split(',')[0];
            var exmTarget = response[i].examples[j].split(',')[1];
            var listr = '<li class="li-animation" style="font-size: 19px; padding:0px; margin: 0px; list-style-type: none;"><span style="color:#0047ab" id=' + srcid + '></span> <span style="color:063145"> &#8594 </span><span id=' + tarid + ' style="color:#00CC00;"></span></li>';
            jQuery("#active-examples").append(listr);
            //jQuery("#source").empty();
            //jQuery("#target").empty();
            var textSource = document.getElementById(srcid);
            var textTarget = document.getElementById(tarid);
            textSource.innerText = exmSource.replace(/_/g, " ");
            textTarget.innerText = exmTarget.replace(/_/g, " ");
        }
    }

    //jQuery("#active-examples").append('<li class="li-animation"><b>Click</b> on Add Edge button to add other instances of the selected edge, connected to the nodes at the two ends of the selected edge.</li>');
    //jQuery("#active-examples").append('<li class="li-animation"><b>Click</b> on Reverse Role button (displayed only when applicable) to reverse the role of source and destination of the edge.</li>');
    //jQuery("#active-examples").append('<li class="li-animation"><b>Close</b> this dialog box and press delete to remove the selected edge.</li>');

    jQuery('#active-examples').each(function() {
        jQuery(this).children().animate({
            left: 0,
            opacity: 1
        });
    });
}

function toggleConfirmButton(mode) {

  var showConfirmButton = false;

  var type = jQuery("#"+mode+"type-options option:selected").text();
  var entity = jQuery("#"+mode+"entity-options option:selected").text();

  var typeExists = false;
  var combinedTypeList =  curTypeList.concat(curEndTypeList);
  for(var elem of combinedTypeList) {
    if(elem.split(':')[2].toLowerCase() == type.toLowerCase()) {
        typeExists = true;
    }
  }

  if(type.toLowerCase() != "select type..." && type != "" && (!typeExists || nodeToBeEdited.entity != -1)) {
    //if a type is selected, show the confirm button only if the type does not already exist in DTL or it was an entity node
    showConfirmButton = true;
  }
  else if(entity.toLowerCase() != "select entity..." && entity != "") {
    showConfirmButton = true;
  }
  else if(entity == "" && (nodeToBeEdited != null && nodeToBeEdited.entity != -1) && combinedTypeList.length != 0) {
    showConfirmButton = true;
  }
  else if(nodeToBeEdited != null) {
    var missingFound = false;
    for(var elem of nodeToBeEdited.typeList) {
      if(curTypeList.contains(elem) == false) {
        missingFound = true;
      }
    }
    if(combinedTypeList.length != 0 && (missingFound || nodeToBeEdited.typeList.length != curTypeList.length)) {
      showConfirmButton = true;
    }
  }
  else if(combinedTypeList.length != 0) {
    showConfirmButton = true;
  }

  if(showConfirmButton) {
    jQuery("#"+mode+"save-changes").css("background-color", "#2ea2cc");
  }
  else {
    jQuery("#"+mode+"save-changes").css("background-color", "#828b8f");
  }
}

function getSubSuggestions() {
    if (subSuggestionsInProgressFlag == true) {
        if ((greyLinkSelected.source.greyflag == 1 && greyLinkSelected.source.flag == 2) || (greyLinkSelected.target.greyflag == 1 && greyLinkSelected.target.flag == 2)) {
            var found = 0;
            var node;
            if (greyLinkSelected.source.greyflag == 0) {
                node = greyLinkSelected.target;
            } else if (greyLinkSelected.target.greyflag == 0) {
                node = greyLinkSelected.source;
            }
            for (var x = 0; x < nodes.length && found == 0; x++) {
                if (nodes[x] === node) {
                    nodes[x].greyflag = 2;
                    nodes[x].flag = 1;
                    found = 1;
                    noOfSuggestionsPicked++;
                    if (noOfSuggestionsPicked == 0 || noOfSuggestionsPicked == 1) {
                        displayFlag = 1;
                    }
                    SugPickdPerGreyLinkCount++;
                    if (SugPickdPerGreyLinkCount == 1 && greyLinkPickdPerSubSuggestion == 0 && noOfSuggestionsPicked > 1) {
                        displayFlag = 1;
                    }
                    animatedHelp();
                    suggestionPicked = true;
                    // button-g already hidden
                }
            }
            restart();
        }
    } else if (subSuggestionsInProgressFlag != true) {
        var i = 0;
        var validCondition = false;
        var found = 0;
        if (addEdgeButtonEdgeAdded == true) {
            validCondition = false;
        } else {
            for (i = 0; i < allReturnObject.length; i++) {
                var entry = allReturnObject[i];
                if (greyLinkSelected.linkID == entry.edge.split('|')[0] && (greyLinkSelected.source.greyflag != 0 || greyLinkSelected.target.greyflag != 0)) {
                    found++;
                    if (found > 1) {
                        validCondition = true;
                        break;
                    };
                }
            }
        }

        if (validCondition == false) {
            if ((greyLinkSelected.source.greyflag == 1 && greyLinkSelected.source.flag == 2) || (greyLinkSelected.target.greyflag == 1 && greyLinkSelected.target.flag == 2)) {
                var found = 0;
                var node;
                if (greyLinkSelected.source.greyflag == 0) {
                    node = greyLinkSelected.target;
                } else if (greyLinkSelected.target.greyflag == 0) {
                    node = greyLinkSelected.source;
                }
                for (var x = 0; x < nodes.length && found == 0; x++) {
                    if (nodes[x] === node) {
                        nodes[x].greyflag = 2;
                        nodes[x].flag = 1;
                        found = 1;
                        noOfSuggestionsPicked++;
                        if (noOfSuggestionsPicked == 0 || noOfSuggestionsPicked == 1) {
                            displayFlag = 1;
                        }
                        suggestionPicked = true;
                        //                      jQuery("#button-g").hide();
                    }
                }
            }
        } else {
            displayFlag = 1;
            animatedHelp();
            validCondition = false;
            for (var j = 0; j < links.length; j++) {
                if (links[j].source.greyflag == 0 && links[j].target.greyflag == 0 && links[j].flag == 4 && links[j].source.id != links[j].target.id) {
                    links[j].flag = 10;
                }
                if (greyLinkSelected != links[j] && links[j].source.id != links[j].target.id) {
                    if (links[j].source.greyflag != 0 || links[j].target.greyflag != 0) {
                        if (links[j].source.greyflag == 0 && links[j].target.greyflag != 0) {
                            if (links[j].target.greyflag == 1 && links[j].target.flag == 2) {
                                for (var m = 0; m < nodes.length; m++) {
                                    if (nodes[m] === links[j].target) {
                                        nodes[m].flag = 9;
                                        nodes[m].greyflag = 9;
                                        break;
                                    }
                                }
                                links[j].target.greyflag = 9;
                                links[j].target.flag = 9;
                            } else if (links[j].target.greyflag == 2 && links[j].target.flag == 1) {
                                for (var m = 0; m < nodes.length; m++) {
                                    if (nodes[m] === links[j].target) {
                                        nodes[m].flag = 10;
                                        nodes[m].greyflag = 10;
                                        break;
                                    }
                                }
                                links[j].target.greyflag = 10;
                                links[j].target.flag = 10;
                            }
                            links[j].flag = 9;
                        } else if (links[j].source.greyflag != 0 && links[j].target.greyflag == 0) {
                            if (links[j].source.greyflag == 1 && links[j].source.flag == 2) {
                                for (var m = 0; m < nodes.length; m++) {
                                    if (nodes[m] === links[j].source) {
                                        nodes[m].flag = 9;
                                        nodes[m].greyflag = 9;
                                        break;
                                    }
                                }
                                links[j].source.greyflag = 9;
                                links[j].source.flag = 9;
                            } else if (links[j].source.greyflag == 2 && links[j].source.flag == 1) {
                                for (var m = 0; m < nodes.length; m++) {
                                    if (nodes[m] === links[j].source) {
                                        nodes[m].flag = 10;
                                        nodes[m].greyflag = 10;
                                        break;
                                    }
                                }
                                links[j].source.greyflag = 10;
                                links[j].source.flag = 10;
                            }
                            links[j].flag = 9;
                        } else {}
                    }
                } else if (greyLinkSelected === links[j]) {
                    var sourceNodeId, targetNodeId;
                    var srcId, trgId;
                    srcId = greyLinkSelected.source.id;
                    trgId = greyLinkSelected.target.id;
                    sourceNodeId = greyLinkSelected.source.nodeID;
                    targetNodeId = greyLinkSelected.target.nodeID;
                    var sourceName = greyLinkSelected.source.name;
                    var targetName = greyLinkSelected.target.name;
                    var linkId = greyLinkSelected.linkID;
                    var linkName = greyLinkSelected.value;
                    var sourceGraphId = greyLinkSelected.source.id;
                    var targetGraphId = greyLinkSelected.target.id;
                    var sourceGreyFlag = greyLinkSelected.source.greyflag;
                    var targetGreyFlag = greyLinkSelected.target.greyflag;
                    var actualSourceType = greyLinkSelected.actualSourceType;
                    var actualTargetType = greyLinkSelected.actualTargetType;
                    var entityValue;
                    if (greyLinkSelected.source.greyflag == 0) {
                        entityValue = greyLinkSelected.target.entity;
                    } else {
                        entityValue = greyLinkSelected.source.entity;
                    }
                    greyLinkDetails.length = 0;
                    var entry = {
                        sourceNodeId: sourceNodeId,
                        targetNodeId: targetNodeId,
                        sourceName: sourceName,
                        targetName: targetName,
                        linkId: linkId,
                        linkName: linkName,
                        sourceGraphId: sourceGraphId,
                        targetGraphId: targetGraphId,
                        sourceGreyFlag: sourceGreyFlag,
                        targetGreyFlag: targetGreyFlag,
                        sourceId: srcId,
                        targetId: trgId,
                        actualSourceType: actualSourceType,
                        actualTargetType: actualTargetType,
                        entity: entityValue
                    };
                    greyLinkDetails.push(entry);
                }
            }
            if (suggestions == true && nodeclick == false) {
                for (i = 0; i < links.length; i++) {
                    if (links[i].source != links[i].target && links[i].flag == 4) {
                        links[i].source = links[i].target;
                    }
                    if (links[i].flag == 20) {
                        links[i].source = links[i].target;
                    }
                }
                nextButtonClick = false;
                //              jQuery("#button-g").hide();
                restart();
            }

            var entry = allReturnObject[0];
            var linkFound = 0;
            var graphS;
            var graphO;
            if (targetGreyFlag == 0) {
                graphS = -1;
                graphO = greyLinkDetails[0].targetId;
            } else if (sourceGreyFlag == 0) {
                graphS = greyLinkDetails[0].sourceId;
                graphO = -1;
            }
            for (i = 0; i < allReturnObject.length; i++) {
                if (greyLinkSelected.linkID == entry.edge.split('|')[0]) {
                    linkFound = 1;
                    subSuggestionsInProgressFlag = true;
                    if (entry.graphSource == graphS && entry.graphObject == graphO && entry.source.split('|')[0] == greyLinkDetails[0].sourceNodeId && entry.object.split('|')[0] == greyLinkDetails[0].targetNodeId && entry.source.split('|')[1] == greyLinkDetails[0].sourceName && entry.object.split('|')[1] == greyLinkDetails[0].targetName && entry.edge.split('|')[0] == greyLinkDetails[0].linkId && entry.edge.split('|')[1] == greyLinkDetails[0].linkName) {
                        // Suggestion Already Exists
                    } else {
                        addNewSubSuggestions(entry);
                    }
                }
                if (i <= (allReturnObject.length - 2)) {
                    var linkId = entry.edge.split('|')[0];
                    entry = allReturnObject[i + 1];
                    if (linkId != entry.edge.split('|')[0] && linkFound == 1) {
                        break;
                    }
                }
            }

        }
        restart();
        var test = nodes.length;
        for (i = 0; i < links.length; i++) {
            if (links[i].flag == 3) {
                jQuery("#linkId_" + links[i].id).css('stroke', '#B0B0B0');
                jQuery("#linkId_" + links[i].id).css('stroke-width', '6px');
            }
        }
        restart();
    }
}



function addNewSubSuggestions(linkEntry) {
    SugPickdPerGreyLinkCount = 0;
    var foundEntry = 0;
    var link;
    var entityValue = 0;
    var entityVal;
    if (greyLinkDetails[0].entity == -1) {
        entityValue = -1;
        entityVal = -1;
    }
    for (var i = 0; i < nodes.length && foundEntry == 0; i++) {
        if (nodes[i].greyflag != 1 && nodes[i].greyflag != 2 && nodes[i].greyflag != 9 && nodes[i].greyfalg != 10) {
            if (nodes[i].id == linkEntry.graphSource && nodes[i].nodeID == linkEntry.source.split('|')[0] && linkEntry.graphObject == -1) {
                if (entityValue != -1) {
                    entityVal = linkEntry.object.split('|')[0];
                }
                var newNode = {
                    id: ++lastNodeId,
                    name: linkEntry.object.split('|')[1],
                    flag: 2,
                    greyflag: 1,
                    nodeID: linkEntry.object.split('|')[0],
                    tempName: linkEntry.object.split('|')[1],
                    entity: entityVal
                };
                var link = {
                    source: nodes[i],
                    target: newNode,
                    value: linkEntry.edge.split('|')[1],
                    id: ++lastEdgeID,
                    flag: 2,
                    linkID: linkEntry.edge.split('|')[0],
                    linkNum: 1,
                    actualSourceType: linkEntry.actualSourceType,
                    actualTargetType: linkEntry.actualObjectType
                };
                nodes.push(newNode);
                links.push(link);
                foundEntry = 1;
            } else if (nodes[i].id == linkEntry.graphObject && nodes[i].nodeID == linkEntry.object.split('|')[0] && linkEntry.graphSource == -1) {
                if (entityValue != -1) {
                    entityVal = linkEntry.object.split('|')[0];
                }
                var newNode = {
                    id: ++lastNodeId,
                    name: linkEntry.source.split('|')[1],
                    flag: 2,
                    greyflag: 1,
                    nodeID: linkEntry.source.split('|')[0],
                    tempName: linkEntry.source.split('|')[1],
                    entity: entityVal
                };
                var link = {
                    source: newNode,
                    target: nodes[i],
                    value: linkEntry.edge.split('|')[1],
                    id: ++lastEdgeID,
                    flag: 2,
                    linkID: linkEntry.edge.split('|')[0],
                    linkNum: 1,
                    actualSourceType: linkEntry.actualSourceType,
                    actualTargetType: linkEntry.actualObjectType
                };
                nodes.push(newNode);
                links.push(link);
                foundEntry = 1;
            } else if (nodes[i].id == linkEntry.graphSource && linkEntry.graphObject != -1) {
                for (var j = 0; j < nodes.length && foundEntry == 0; j++) {
                    if (nodes[j].id == linkEntry.graphObject) {
                        var link = {
                            source: nodes[i],
                            target: nodes[j],
                            value: linkEntry.edge.split('|')[1],
                            id: ++lastEdgeID,
                            flag: 3,
                            linkID: linkEntry.edge.split('|')[0],
                            linkNum: 1,
                            actualSourceType: linkEntry.actualSourceType,
                            actualTargetType: linkEntry.actualObjectType
                        };
                        for (var k = 0; k < links.length; k++) {
                            if (links[k].source.id == link.source.id && links[k].target.id == link.target.id && links[k].source != links[k].target)
                                link.linkNum++;
                        }
                        links.push(link);
                        foundEntry = 1;
                    }
                }
            } else if (nodes[i].id == linkEntry.graphObject && linkEntry.graphSource != -1) {
                for (var j = 0; j < nodes.length && foundEntry == 0; j++) {
                    if (nodes[j].id == linkEntry.graphSource) {
                        var link = {
                            source: nodes[j],
                            target: nodes[i],
                            value: linkEntry.edge.split('|')[1],
                            id: ++lastEdgeID,
                            flag: 3,
                            linkID: linkEntry.edge.split('|')[0],
                            linkNum: 1,
                            actualSourceType: linkEntry.actualSourceType,
                            actualTargetType: linkEntry.actualObjectType
                        };
                        for (var k = 0; k < links.length; k++) {
                            if (links[k].source.id == link.source.id && links[k].target.id == link.target.id && links[k].source != links[k].target)
                                link.linkNum++;
                        }
                        links.push(link);
                        foundEntry = 1;
                    }
                }
            } else {}
        }
    }
}



/*jQuery('#myModal').keydown(function(e) {

    if (e.keyCode == 8) { // 8 is backspace
        e.preventDefault();
     }

});*/


jQuery('#domain-options').click(function() {

    var text = jQuery("#domain-options option:selected").text();
    var value = jQuery("#domain-options option:selected").val();

    jQuery("#type-selected-value").attr('value', -1);
    jQuery("#type-selected").attr('value', "Select Type...");
    jQuery("#entity-selected-value").attr('value', -1);
    jQuery("#entity-selected").attr('value', "Select Entity...");


    jQuery("#domain-selected").attr('value', text);
    jQuery("#domain-selected").attr('title', text);

    if(value == prevDomSel) {
      deselectDomain();
      prevDomSel = null;
      return;
    }

    typesThruDomainFlag = 1;
    entitiesThruDomainFlag = 1

    prevDomSel = value
    if(curNodeLabel == null) curNodeLabel = selected_node.name;
    selected_node.name = text;
    selected_node.tempName = text;
    selected_node.nodeID = value;
    selected_node.domain = value;

    if (text != "Select Domain...") {
        checkKeywordStatus();
        typesThroughDomain.length = 0;
        entitiesThroughDomain.length = 0;

        if(curTypeList.concat(curEndTypeList).length != 0) {
          updateDomainsTypesEntities("", "");
          typesThroughDomain = generatedTypes.slice();
          entitiesThroughDomain = generatedEntities.slice();
        } else {
          getRequest("https://" + urlFull + "/orion_app/gettypes?domain=" + value + "&windownum=0" + "&windowsize=" + typeWindowSize + "&keyword=", typesThroughDomain);
          getRequest("https://" + urlFull + "/orion_app/getentities?domain=" + value + "&windownum=0" + "&windowsize=" + entityWindowSize + "&type=-1" + "&keyword=", entitiesThroughDomain);
        }


        //jQuery('#domain-options').find('[value="' + jQuery("#domain-selected-value").val() + '"]').prop('selected', true);

        displayTypeOptions(typesThroughDomain);
        displayEntityOptions(entitiesThroughDomain);

        // if(curTypeList.concat(curEndTypeList).length != 0) {
        //   var allTypesForSelectedDomain = [];
        //   var typeWindowNumber = typeCount-1;
        //   var keywordValue = "";
        //
        //   getRequest("https://" + urlFull + "/orion_app/gettypes?domain=" + value + "&windownum=" + typeWindowNumber + "&windowsize=" + typeWindowSize + "&keyword=" + keywordValue, allTypesForSelectedDomain);
        //   var nodeTypeValues = "";
        //   for(var elem1 of types) {
        //     for(var elem2 of allTypesForSelectedDomain) {
        //       if(elem1[0]==elem2[0]) {
        //         typesThroughDomain.push(elem1);
        //         if(nodeTypeValues == "") {
        //           nodeTypeValues = elem1[0];
        //         }
        //         else {
        //           nodeTypeValues = nodeTypeValues + "," + elem1[0];
        //         }
        //         break;
        //       }
        //     }
        //   }
        //
        //   var entityWindowNumber = entityCount - 1;
        //   getRequest("https://" + urlFull + "/orion_app/getunionofentitiesfortypes?nodetypevalues=" + nodeTypeValues + "&entityewindownum=" + entityWindowNumber + "&windowsize=" + entityWindowSize, entitiesThroughDomain);
        // }
        // else {
        //   getRequest("https://" + urlFull + "/orion_app/gettypes?domain=" + value + "&windownum=0" + "&windowsize=" + typeWindowSize + "&keyword=", typesThroughDomain);
        //   getRequest("https://" + urlFull + "/orion_app/getentities?domain=" + value + "&windownum=0" + "&windowsize=" + entityWindowSize + "&type=-1" + "&keyword=", entitiesThroughDomain);
        // }
    }
    // else {
    //     checkKeywordStatus();
    //     displayTypeOptions(types);
    //     displayEntityOptions(entities);
    // }


    // if (text != "Select Domain...") {
    //     jQuery("#domain-selected").show();
    //     jQuery("#domain-x-button").show();
    //
    // }
    jQuery("#type-selected").hide();
    jQuery("#type-x-button").hide();
    jQuery("#entity-x-button").hide();
    jQuery("#entity-selected").hide();
    jQuery("#add-type-text").hide();
    jQuery("#add-type-button").hide();
    toggleConfirmButton("");
    restart();
});



function deselectDomain() {
    typesThruDomainFlag = 0;
    entitiesThruTypeFlag = 0;
    entitiesThruDomainFlag = 0;
    jQuery("#domain-options option:selected").attr('selected', false);
    jQuery("#domain-selected-value").attr('value', -1);
    jQuery("#domain-selected").attr('value', "Select Type...");
    jQuery("#domain-selected").hide();
    jQuery("#domain-x-button").hide();
    jQuery("#type-selected-value").attr('value', -1);
    jQuery("#type-selected").attr('value', "Select Type...");
    jQuery("#type-selected").hide();
    jQuery("#type-x-button").hide();
    jQuery("#entity-selected-value").attr('value', -1);
    jQuery("#entity-selected").attr('value', "Select Entity...");
    jQuery("#entity-selected").hide();
    jQuery("#entity-x-button").hide();
    checkKeywordStatus();
    displayTypeOptions(types);
    displayEntityOptions(entities);
    restart();
}



function deselectType() {
    jQuery("#add-type-text").hide();
    jQuery("#add-type-button").hide();
    entitiesThruTypeFlag = 0;
    jQuery("#type-options option:selected").attr('selected', false);
    jQuery("#type-selected-value").attr('value', -1);
    jQuery("#type-selected").attr('value', "Select Type...");
    jQuery("#type-selected").hide();
    jQuery("#type-x-button").hide();
    jQuery("#entity-selected-value").attr('value', -1);
    jQuery("#entity-selected").attr('value', "Select Entity...");
    jQuery("#entity-selected").hide();
    jQuery("#entity-x-button").hide();

    toggleConfirmButton("");

    restart();
}



function deselectEntity() {
    jQuery("#entity-options option:selected").attr('selected', false);
    jQuery("#entity-selected-value").attr('value', -1);
    jQuery("#entity-selected").attr('value', "Select Entity...");
    jQuery("#entity-selected").hide();
    jQuery("#entity-x-button").hide();
    // if (entitiesThruTypesFlag == 1) {
    //     addEntities();
    // } else if (entitiesThruDomainFlag == 1)
    //
    // {
    //     //jQuery("#save-changes").css("background-color", "#828b8f");
    //     addTypesAndEntities();
    // } else {
    //     //jQuery("#save-changes").css("background-color", "#828b8f");
    //     displayEntityOptions(entities);
    // }
    toggleConfirmButton("");
    restart();
}

function setNodeDetails(entity, type, domain) {
    var text;
    var value;
    selected_node.entity = -1;
    selected_node.type = -1;
    selected_node.domain = -1;

    if (entity == 1) {
        /*text = jQuery("#entity-options option:selected").text();
        value = jQuery("#entity-options option:selected").val();*/
        text = jQuery("#entity-selected").val();
        value = jQuery("#entity-selected-value").val();
        selected_node.entity = value;
        if (type == 1) {
            //selected_node.type = jQuery("#type-options option:selected").val();}
            selected_node.type = jQuery("#type-selected-value").val();
        }
        if (domain == 1) {
            selected_node.domain = jQuery("#domain-options option:selected").val();
        }
        selected_node.name = text;
        selected_node.tempName = text;
        selected_node.nodeID = value;
    } else if (entity == 0 && type == 1) {
        /*text = jQuery("#type-options option:selected").text();
        value = jQuery("#type-options option:selected").val();*/
        //text = jQuery("#type-selected").val();
        //value = jQuery("#type-selected-value").val();
        selected_node.type = parseInt(selected_node.typeList[0].split(':')[0]);
        if (domain == 1) {
            selected_node.domain = jQuery("#domain-options option:selected").val();
        }

        var typeLabels = [];
        for(var elem of selected_node.typeList) {
          typeLabels.push(elem.split(':')[2]);
        }
        selected_node.tempName = typeLabels.join(' & ');
        selected_node.name = typeLabels.join(' & ');
        selected_node.nodeID = parseInt(selected_node.typeList[0].split(':')[0]);
    }
}


jQuery('#type-options').click(function() {

    var text = jQuery("#type-options option:selected").text();
    if (text == "Select Type...") return;
    var value = jQuery("#type-options option:selected").val();
    if(value == prevTypSel) {
      deselectType();
      prevTypSel = null;
      //update entity column when a type is deselected
      if(typesThruDomainFlag) {
        displayEntityOptions(entitiesThroughDomain);
      } else {
        displayEntityOptions(entities);
      }
      return;
    }

    entitiesThruTypeFlag = 1;

    prevTypSel = value;
    var domainName = getDomainForType(value);
    var typeExists = false;
    for(var elem of curTypeList.concat(curEndTypeList)) {
        if(elem.split(':')[0] == value) {
            typeExists = true;
        }
    }
    if(typeExists == false) {
        //jQuery("#add-type-button").html("Add " + domainName[1] + ":" + text + " to Domain:Type list").show();
        jQuery("#add-type-text").html("Require this node to be in Type <b>"+text.toUpperCase()+"</b> (in Domain <i><b>"+domainName[1].toUpperCase()+"</b></i>)?").show();
        jQuery("#add-type-button").show();
    }
    else {
        jQuery("#add-type-text").hide();
        jQuery("#add-type-button").hide();
    }
    // var text2 = jQuery("#domain-options option:selected").text();
    // if (selected_node.name == "select Name")

    if(curNodeLabel == null) curNodeLabel = selected_node.name;
    selected_node.name = text;
    selected_node.tempName = text;
    selected_node.nodeID = value;
    selected_node.type = value;
    var ele = document.getElementById('type-selected');
    ele.size = text.length;
    if (text.length > 50) {
        ele.size = 50;
    }
    jQuery("#type-selected-value").attr('value', value);
    jQuery("#type-selected").attr('value', text);
    jQuery("#type-selected").attr('title', text); //Edited by AD
    jQuery("#entity-selected-value").attr('value', -1);
    jQuery("#entity-selected").attr('value', "Select Entity...");
    // selected_node.domain =
    jQuery("#entity-options").empty();
    jQuery("#entity-options").attr('size', 1);
    if (text != "Select Type...") {
        //jQuery("#save-changes").css("background-color", "#2ea2cc");
        if(curTypeList.concat(curEndTypeList).length != 0) {
          updateDomainsTypesEntities("", "");
          entitiesThroughType = generatedEntities.slice();
          jQuery('#type-options').find('[value="' + jQuery("#type-selected-value").val() + '"]').prop('selected', true);
        } else {
          addEntities();
        }
        displayEntityOptions(entitiesThroughType);
    }

    // else if (text2 != "Select Domain...")
    // {
    //     addTypesAndEntities();}
    // else {
    //     displayEntityOptions(entities);
    // }
    toggleConfirmButton("");
    checkKeywordStatus();
    // <!-- Edited by AD
    jQuery("#entity-selected-value").attr('value', -1);
    jQuery("#entity-selected").attr('value', "Select Entity...");
    // jQuery("#type-selected").attr('value', text);
    // jQuery("#type-selected").attr('title', text);
    // -->
    // if (text != "Select Type...") {
    //     jQuery("#type-selected").show();
    //     jQuery("#type-x-button").show();
    // }
    jQuery("#entity-selected").hide();
    jQuery("#entity-x-button").hide();
    //jQuery("#add-type-button").show();
    // var ot = jQuery('#domain-options').find('[value="' + domainName[0] + '"]').offset().top;
    // var st = jQuery('#domain-options').offset().top;
    // jQuery('#domain-options').scrollTop(jQuery('#domain-options').scrollTop() + ot - st);
    // jQuery('#domain-options').find('[value="' + domainName[0] + '"]').prop('selected', true);
    //
    // jQuery("#domain-selected").attr('value', domainName[1]);
    // jQuery("#domain-selected").attr('title', domainName[1]);
    // if (text != "Select Domain...") {
    //     jQuery("#domain-selected").show();
    //     jQuery("#domain-x-button").show();
    // }

    restart();
});


jQuery('#entity-options').click(function() {
    var text = jQuery("#entity-options option:selected").text();
    var value = jQuery("#entity-options option:selected").val();
    if(value == prevEntSel) {
      deselectEntity();
      prevEntSel = null;
      return;
    }
    prevEntSel = value;
    if(curNodeLabel == null) curNodeLabel = selected_node.name;
    selected_node.name = text;
    selected_node.tempName = text;
    selected_node.nodeID = value;
    selected_node.entity = value;
    console.log(value + "\n" + selected_node.name);
    var ele = document.getElementById('entity-selected');
    ele.size = text.length;
    if (text.length > 50) {
        ele.size = 50;
    }
    jQuery("#entity-selected-value").attr('value', value);
    if (text[0] == "") {
        text = text.substring(2);
    }
    jQuery("#entity-selected").attr('value', text);
    jQuery("#entity-selected").attr('title', text);
    checkKeywordStatus();
    toggleConfirmButton("");
    restart();
});


jQuery('#edge-options').change(function() {
    var val = jQuery("#edge-options option:selected").text();
    var value = jQuery("#edge-options option:selected").val();

    if (val != null)
        selected_link.value = val;

    if (value != null) {
        selected_link.linkID = value;
    };

    console.log(val + "\n" + selected_link.value);

    restart();

});

// jQuery("#button-g").click(function(){
//   nextButtonClick= true;
// });

jQuery("#submit-button").click(function() {
    endDateVar = new Date();
    endTimeVar = endDateVar.getTime();
    var diff = (endTimeVar - startTimeVar) / 1000;
    document.getElementById('entry_978148691').value = endTimeVar;
    document.getElementById('entry_1404241487').value = diff;
    if (nodes.length == 0) return;

    jQuery("#surveyForm").modal('show');

});

jQuery("#clear-button").click(function() {
    jQuery("#wrap5").toggleClass('active');
    if (clearButtonFlag == 1) {
        clearButtonFlag = 0;
    } else {
        clearButtonFlag = 1;
    }
    if (usefulTipsFlag == 1) {
        jQuery("#wrap2").toggleClass('active');
        jQuery("#useful-tips").toggleClass('active');
        usefulTipsFlag = 0;
    }
    if (settingsFlag == 1) {
        jQuery("#wrap3").toggleClass('active');
        jQuery("#settings").toggleClass('active');
        settingsFlag = 0;
    }
    if (edgeTypesFlag == 1) {
        jQuery("#wrap4").toggleClass('active');
        jQuery("#edge-types").toggleClass('active');
        edgeTypesFlag = 0;
    }


});



jQuery("#cancel-clear").click(function() {
    jQuery("#wrap5").toggleClass('active');
    clearButtonFlag = 0
});

jQuery("#clear-graph").click(function() {
    nodes.length = 0;
    links.length = 0;
    lastNodeId = 2;
    lastEdgeID = 2;
    partialGraph.length = 0;
    rejectedGraph.length = 0;
    returnObject.length = 0;
    allReturnObject.length = 0;
    disconnectedGraph = false;
    suggestionCounter = 0;
    noOfSuggestionsPicked = 0;
    suggestionsFinalized = false;
    suggestionPicked = false;
    nextButtonClick = false;
    subSuggestionsInProgressFlag = false;
    nodeclick = false;
    allowNodeCreation = true;
    newNode = false;
    jQuery("#useful-tips-list").show();
    jQuery("#modal-text").empty();
    showInstructionsFlag = 0;
    newEdge = false;
    //   drawEdge = false;
    suggestions = false;
    greyLinkSelectedFlag = 0;
    greyLinkSelected = null;
    greyLinkSuggestionsPicked = 0;
    SugPickdPerGreyLinkCount = 0;
    greyLinkPickdPerSubSuggestion = 0;
    linkSuggestionsFinalized = false;
    greyLinkDetails.length = 0;
    exampleReverseRoleFlag = false;
    exampleActualSourceType = null;
    exampleActualTargetType = null;
    addEdgeButtonEdgeAdded = false;
    exampleSource = null;
    exampleTarget = null;
    exampleLinkId = -1;
    exampleLinkGraphId = -1;
    jQuery("#active-examples").empty();
    //   jQuery("#button-g").hide();
    jQuery("#example-text").show();
    jQuery("#Reverse-button").hide();
    jQuery("#Add-Edge-button").hide();
    response.length = 0;
    hideDragLineFlag = false;
    clearIndexGlobalVariables();
    jQuery('defs').empty();
    jQuery('#pathG').empty();
    jQuery('#circleG').empty();
    jQuery('#linktextg').empty();
    // jQuery("#queryConstructSVG").empty();
    // var refresh = svg.append("svg:foreignObject")
    //             .attr("width", 100)
    //             .attr("height", 100)
    //             .attr("y", "15")
    //             .attr("x", x)
    //             .append("xhtml:span")
    //             .attr("class", "control glyphicon glyphicon-zoom-in")
    //             .on('click',function(){
    //               alert("ASdasd");
    //             })== 3

    jQuery.ajax({
        url: "https://" + urlFull + "/orion_app/clearcanvas?sessionid=" + sessionId,
        type: 'get',
        async: false,
        success: function(response) {}
    });
    queryWindowNum = 1;
    document.getElementById("query-keyword").value = "";
    jQuery("#query-result-table tr").remove();
    jQuery("#query-result-modal").hide();
    jQuery("#wrap5").toggleClass('active');
    clearButtonFlag = 0
    animatedHelp();
    jQuery("#button-g").attr("class", "inactive");
    jQuery("#submit-button").attr("class", "inactive");
});


function mousedown() {
    if (clearButtonFlag == 1 || usefulTipsFlag == 1 || settingsFlag == 1 || edgeTypesFlag == 1) {
        if (usefulTipsFlag == 1) {
            usefulTipsFlag = 0;
            if (timeInitialisationFlag == 0) {
                startDateVar = new Date();
                startTimeVar = startDateVar.getTime();
                if (partialGraph.length <= 0 && rejectedGraph.length <= 0) {
                    document.getElementById('entry_914362311').value = startTimeVar;
                }
                timeInitialisationFlag = 1;
            }
            jQuery("#wrap2").toggleClass('active');
            jQuery("#useful-tips").toggleClass('active');
        }
        if (clearButtonFlag == 1) {
            clearButtonFlag = 0;
            jQuery("#wrap5").toggleClass('active');
        }
        if (settingsFlag == 1) {
            settingsFlag = 0;
            jQuery("#wrap3").toggleClass('active');
            jQuery("#settings").toggleClass('active');
        }
        if (edgeTypesFlag == 1) {
            edgeTypesFlag = 0;
            jQuery("#wrap4").toggleClass('active');
            jQuery("#edge-types").toggleClass('active');

            if (exampleReverseRoleFlag == true && addEdgeButtonEdgeAdded == false) {
                jQuery("#linkId_" + exampleLinkGraphId).css('stroke', 'black');
                exampleSource = null;
                exampleTarget = null;
                exampleLinkId = -1;
                exampleLinkGraphId = -1;
                exampleReverseRoleFlag = false;
                animatedHelp();
                ExampleActualSourceType = null;
                exampleActualTargetType = null;
                addEdgeButtonEdgeAdded = false;
                jQuery("#example-text").show();
                jQuery("#Add-Edge-button").hide();
                jQuery("#active-examples").empty();
                jQuery("#Reverse-button").hide();
                restart();
            }
        }
    } else {
        // prevent I-bar on drag
        //d3.event.preventDefault();

        // because :active only works in WebKit?
        svg.classed('active', true);
        if (timeInitialisationFlag == 0) {
            startDateVar = new Date();
            startTimeVar = startDateVar.getTime();
            if (partialGraph.length <= 0 && rejectedGraph.length <= 0) {
                document.getElementById('entry_914362311').value = startTimeVar;
            }
            timeInitialisationFlag = 1;
        }
        //   if (nextButtonClick) {
        //     var nextClickMode = 1;
        //     nextClick(nextClickMode);
        //   }else{
        if (subSuggestionsInProgressFlag == false)
        //     {
        //        if(d3.event.ctrlKey || mousedown_node || mousedown_link) return;
        // //        removeSubSuggestionsTempNodes();
        //        if (SugPickdPerGreyLinkCount == 0 && greyLinkPickdPerSubSuggestion == 0){
        //           addGreyLinkSelectedAgain();
        //        }
        //        if (greyLinkSuggestionsPicked == 0 && suggestionPicked == false){
        //           jQuery("#button-g").show();
        //        }
        //       /* if (((noOfSuggestionsPicked > 1) && SugPickdPerGreyLinkCount == 0)|| SugPickdPerGreyLinkCount == 1){
        //             DisplayAnimatedHelp();
        //        }*/
        //        SugPickdPerGreyLinkCount = 0;
        //        greyLinkPickdPerSubSuggestion = 0;
        //        subSuggestionsInProgressFlag = false;
        //        animatedHelp();
        //        greyLinkSelectedFlag = 0;
        //        greyLinkSelected = null;
        //        greyLinkDetails.length = 0;
        //        restart();
        //     }
        //     else
        {
            if (d3.event.ctrlKey || mousedown_node || mousedown_link) return;

            // insert new node at point
            if (!d3.event.shiftKey && allowNodeCreation) {
                jQuery("#selectDiv").show();
                var point = d3.mouse(this);
                disconnectedGraph = checkConnected(nodes, links);
                //           removeTempNodes();
                restart();
                if (!disconnectedGraph) {
                    restart();
                };
                if (exampleReverseRoleFlag == true) {
                    jQuery("#linkId_" + exampleLinkGraphId).css('stroke', 'black');
                    exampleSource = null;
                    exampleTarget = null;
                    exampleLinkId = -1;
                    exampleLinkGraphId = -1;
                    exampleReverseRoleFlag = false;
                    animatedHelp();
                    ExampleActualSourceType = null;
                    exampleActualTargetType = null;
                    addEdgeButtonEdgeAdded = false;
                    jQuery("#example-text").show();
                    jQuery("#Add-Edge-button").hide();
                    jQuery("#active-examples").empty();
                    jQuery("#Reverse-button").hide();
                }
                if (greyLinkSuggestionsPicked > 0) {
                    //              linkSuggestionsFinalized = true;
                    greyLinkSuggestionsPicked = 0;
                }
                if (linkSuggestionsFinalized == false && !translateAllowed)
                    createNewNode();
                else if(translateAllowed)
                    jQuery("#myModal4").modal("show");

                if ((linkSuggestionsFinalized == true) && !nodeclick) {
                    //              suggestionsFinalized = true;
                    suggestionCounter = 0;
                    restart();
                    var mouseDownMode = 2;
                    addSuggestions(0, mouseDownMode);
                };
            }

            //     }
        }
        // animatedHelp();
    }

}


function addGreyLinkSelectedAgain() {

    for (var j = 0; j < nodes.length; j++) {
        if (nodes[j].nodeID == greyLinkDetails[0].sourceNodeId && greyLinkDetails[0].sourceGreyFlag == 0 && nodes[j].id == greyLinkDetails[0].sourceGraphId && nodes[j].greyflag == 0) {
            var newNode = {
                id: ++lastNodeId,
                name: greyLinkDetails[0].targetName,
                flag: 2,
                greyflag: 1,
                nodeID: greyLinkDetails[0].targetNodeId,
                tempName: greyLinkDetails[0].targetName,
                entity: greyLinkDetails[0].entity
            };
            var link = {
                source: nodes[j],
                target: newNode,
                value: greyLinkDetails[0].linkName,
                id: ++lastEdgeID,
                flag: 2,
                linkID: greyLinkDetails[0].linkId,
                linkNum: 1,
                actualSourceType: greyLinkDetails[0].actualSourceType,
                actualTargetType: greyLinkDetails[0].actualTargetType
            };
            nodes.push(newNode);
            links.push(link);

        } else if (nodes[j].nodeID == greyLinkDetails[0].targetNodeId && greyLinkDetails[0].targetGreyFlag == 0 && nodes[j].id == greyLinkDetails[0].targetGraphId && nodes[j].greyflag == 0) {
            var newNode = {
                id: ++lastNodeId,
                name: greyLinkDetails[0].sourceName,
                flag: 2,
                greyflag: 1,
                nodeID: greyLinkDetails[0].sourceNodeId,
                tempName: greyLinkDetails[0].sourceName,
                entity: greyLinkDetails[0].entity
            };
            var link = {
                source: newNode,
                target: nodes[j],
                value: greyLinkDetails[0].linkName,
                id: ++lastEdgeID,
                flag: 2,
                linkID: greyLinkDetails[0].linkId,
                linkNum: 1,
                actualSourceType: greyLinkDetails[0].actualSourceType,
                actualTargetType: greyLinkDetails[0].actualTargetType
            };
            nodes.push(newNode);
            links.push(link);

        }
    }

}



function removeSubSuggestionsTempNodes() {
    if (suggestions == true && nodeclick == false) {
        for (var i = 0; i < nodes.length; i++) {
            if (nodes[i].greyflag == 1 && nodes[i].flag == 2) {
                var suggestFlagTemp = suggestionPicked;
                var nxtBtnClkTemp = nextButtonClick;
                suggestionPicked = false;
                nextButtonClick = false;
                spliceLinksForNode(nodes[i]);
                nodes.splice(nodes.indexOf(nodes[i]), 1);
                suggestionPicked = suggestFlagTemp;
                nextButtonClick = nxtBtnClkTemp;
                i--;
            };
        };
    }
    for (var k = 0; k < nodes.length; k++) {
        if (nodes[k].flag == 9 && nodes[k].greyflag == 9) {
            nodes[k].greyflag = 1;
            nodes[k].flag = 2;
        } else if (nodes[k].flag == 10 && nodes[k].greyflag == 10) {
            nodes[k].greyflag = 2;
            nodes[k].flag = 1;
        }
    }
    selected_node = null;
    selected_link = null;
    for (var i = 0; i < links.length; i++) {
        if (links[i].flag == 9) {
            if (links[i].target.greyflag == 0 && links[i].source.greyflag == 9 && links[i].source.flag == 9) {
                links[i].source.greyflag = 1;
                links[i].source.flag = 2;
            } else if (links[i].target.greyflag == 0 && links[i].source.greyflag == 10 && links[i].source.flag == 10) {
                links[i].source.greyflag = 2;
                links[i].source.flag = 1;
            } else if (links[i].source.greyflag == 0 && links[i].target.greyflag == 9 && links[i].target.flag == 9) {
                links[i].target.greyflag = 1;
                links[i].target.flag = 2;
            } else if (links[i].source.greyflag == 0 && links[i].target.greyflag == 10 && links[i].target.flag == 10) {
                links[i].target.greyflag = 2;
                links[i].target.flag = 1;
            }

            links[i].flag = 2;
            jQuery("#linkId_" + links[i].id).css('stroke', 'black');
        } else if (links[i].flag == 10) {
            links[i].flag = 4;
        }
        if (links[i].flag == 3 && links[i].source.greyflag == 0 && links[i].target.greyflag == 0) {
            /*var srcNodeId = links[i].source.nodeID;
            var tarNodeId = links[i].target.nodeID;
            var graphSrc = links[i].source.id;
            var graphTarget = links[i].target.id;
            rejectedGraph.push({source: srcNodeId, graphSource: graphSrc,  edge: links[i].linkID, object: tarNodeId, graphObject: graphTarget});*/
            links[i].source = links[i].target;
            links[i].flag = 2;
        }
        if (links[i].flag == 20) {
            links[i].source = links[i].target;
        }
    }
    restart()
}




function mousemove() {
    if (!mousedown_node) return;

    // update drag line
    //  drag_line.attr('d', 'M' + mousedown_node.x + ',' + mousedown_node.y + 'L' + d3.mouse(this)[0] + ',' + d3.mouse(this)[1]);
    drag_line.attr('d', 'M' + mousedown_node.x + ',' + mousedown_node.y + 'A' + 0 + ',' + 0 + ' 0 0,1 ' + d3.mouse(this)[0] + ',' + d3.mouse(this)[1]);
    restart();
}

function createNewNode() {
    //   if (disconnectedGraph == false) {
    updateUndo();
    node = {
        id: ++lastNodeId,
        reflexive: false,
        name: " ",
        flag: 1,
        greyflag: 0,
        nodeID: -1,
        domain: "",
        type: "",
        entity: "-1",
        tempName: " ",
        typeList: []
    };
    selected_node = node;
    newNode = true;
    nodes.push(node);
    selected_node = node;
    var graphDisconnect = checkConnected(nodes, links);
    if (graphDisconnect == true) {
        //         jQuery("#button-g").hide();
    }
    jQuery("#type-selected-value").attr('value', -1);
    jQuery("#type-selected").attr('value', "Select Type...");
    jQuery("#entity-selected-value").attr('value', -1);
    jQuery("#entity-selected").attr('value', "Select Entity...");
    jQuery("#domain-selected-value").attr('value', -1);
    jQuery("#domain-selected").attr('value', "Select Domain...");
    jQuery("#type-selected").hide();
    jQuery("#type-x-button").hide();
    jQuery("#entity-x-button").hide();
    jQuery("#entity-selected").hide();
    jQuery("#domain-x-button").hide();
    jQuery("#domain-selected").hide();

    clearIndexGlobalVariables();
    clearAllSelection();

    displayDomainOptions(allDomains);
    displayTypeOptions(allTypes);
    displayEntityOptions(allEntities);

    domains = allDomains.slice();
    types = allTypes.slice();
    entities = allEntities.slice();

    setToFirstPage();

    jQuery("#modal-title").empty();
    jQuery("#modal-title").append("<h4>Create a New Node</h4>");
    jQuery("#domain-options").prop('disabled', false);
    jQuery("#type-options").prop('disabled', false);
    jQuery("#entity-options").prop('disabled', false);
    jQuery("#modal-text").empty();
    jQuery('#myModal').modal('show');
    jQuery("#add-type-text").hide();
    jQuery("#add-type-button").hide();
    jQuery("#type-list").html(tdListText).show();
    jQuery("#dtl tr:gt(0)").remove();
    jQuery("#dtl").show();
    curTypeList = [];
    curEndTypeList = [];
    curNodeLabel = null;
    prevDomSel = null;
    prevTypSel = null;
    prevEntSel = null;
    //   }
}


function getNodeToBeEdited() {
    return nodeToBeEdited;
}

function getNodeEditEdges() {
    return nodeEditEdges;
}


function setNodeEditKeywordValue(search) {
    editNodeKeyword = search;
}

function setWindowNoOfNodeEdit(windowNo) {
    nodeEditWindowNo = windowNo;
}

function setPreviousNodeEditValues() {
    var value = jQuery("#edit-type-selected-value").val();
    var domainVal = jQuery("#edit-domain-selected-value").val();
    if (!domainVal) {
        domainVal = -1;
    }
    if (!value) {
        value = -1;
    }
    loadEditEntityList(domainVal, value);
    //     jQuery("#node-edit-entity-options").attr('size',19);
    //     jQuery("#node-edit-entity-options").empty();

    //     for (var i = 0; i < nodeEditList.length; i++) {
    //       if(nodeEditList[i][nodeEditList[i].length-1]=="preview"){
    //            jQuery("#node-edit-entity-options").append('<option value="'+nodeEditList[i][0]+'" id="entity-value-1" class="fas" data-toggle="tooltip" data-container="#tooltip_container">&#xf05a;&nbsp'+nodeEditList[i].slice(1,nodeEditList[i].length-1).join()+'</option>'); //Changed by Heet
    //       }
    //       else{

    //            jQuery("#node-edit-entity-options").append('<option value="'+nodeEditList[i][0]+'" id="entity-value-1" class="fas" >'+nodeEditList[i].slice(1,nodeEditList[i].length-1).join()+'</option>'); //Changed by Heet
    //        }
    // //         jQuery("#node-edit-entity-options").append('<option value="'+nodeEditList[i][0]+'" id="add-value-1" >'+nodeEditList[i][1]+'</option>');
    //     };
    //     nodeEditWindowNo = 0;
    //     jQuery("#node-edit-entity-back").attr("disabled",true);
    //     jQuery("#node-edit-entity-next").attr("disabled",false);
    //     if (nodeEditList.length <= windowSize){
    //          jQuery("#node-edit-entity-next").attr("disabled",true);
    //     }
}


function setPreviousNodeEditTypeValues() {
    var domainVal = jQuery("#node-edit-domain-options option:selected").val();
    if (!domainVal) {
        domainVal = -1;
    }
    loadEditTypeList(domainVal);

}


jQuery("#node-edit-type-back").click(function() {
    event.stopPropagation();

    if (nodeEditTypeWindowNo > 0) {
        nodeEditTypeWindowNo--;

        var domainVal = jQuery("#node-edit-domain-options option:selected").val();

        if (!domainVal) {
            domainVal = -1;
        }
        loadEditTypeList(domainVal);
    }
});


jQuery("#node-edit-type-next").click(function() {
    event.stopPropagation();

    nodeEditTypeWindowNo++;

    var domainVal = jQuery("#node-edit-domain-options option:selected").val();

    if (!domainVal) {
        domainVal = -1;
    }
    loadEditTypeList(domainVal);
    jQuery("#node-edit-type-back").attr("disabled", false);
});

jQuery("#node-edit-entity-back").click(function() {
    event.stopPropagation();

    if (nodeEditWindowNo >= 0) {
        nodeEditWindowNo--;
        var typeVal = jQuery("#edit-type-selected-value").val();
        var domainVal = jQuery("#edit-domain-selected-value").val();
        if (!typeVal) {
            typeVal = -1;
        }
        if (!domainVal) {
            domainVal = -1;
        }
        loadEditEntityList(domainVal, typeVal);
    }
    //     jQuery("#node-edit-entity-next").attr("disabled",false);
    //     // jQuery('#loading').modal('show');
    //     // setTimeout(function(){
    //     //   jQuery('#loading-indicator').show();
    //       nodeEditWindowNo--;
    //       editList.length = 0;
    //        var text = jQuery("#node-edit-type-options option:selected").text();
    //     value = jQuery("#node-edit-type-options option:selected").val();
    //     if(text.toLowerCase() != "select type..." && text != ""){
    //       getRequest("https://" + urlFull + "/orion_app/geteditnode?node="+value+"&windownum="+nodeEditWindowNo+"&windowsize="+windowSize+"&edges=&keyword="+editNodeKeyword, editList);
    //     }
    //     else{
    //       getRequest("https://" + urlFull + "/orion_app/geteditnode?node="+nodeToBeEdited.nodeID+"&windownum="+nodeEditWindowNo+"&windowsize="+windowSize+"&edges="+nodeEditEdges+"&keyword="+editNodeKeyword, editList);
    //     }

    // //       getRequest("https://" + urlFull + "/orion_app/geteditnode?node="+nodeToBeEdited.nodeID+"&windownum="+nodeEditWindowNo+"&windowsize="+windowSize+"&edges="+nodeEditEdges+"&keyword="+editNodeKeyword, editList);
    //       if(editList.length < 19){
    //           jQuery("#node-edit-entities-options").attr('size',editList.length);}
    //       else{
    //           jQuery("#node-edit-entity-options").attr('size',entityDefaultSize);
    //       }
    //       if (editList.length <= windowSize){
    //          jQuery("#node-edit-entity-next").attr("disabled", true);}
    //       jQuery("#node-edit-entity-options").empty();
    // //       for (var i = 0; i < editList.length; i++) {
    // //           jQuery("#node-edit-entity-options").append('<option value="'+editList[i][0]+'" id="entity-value-1">'+editList[i][1]+'</option>');
    // //     };
    //     for (var i = 0; i < editList.length; i++) {
    //       if(editList[i][editList[i].length-1]=="preview"){
    //            jQuery("#node-edit-entity-options").append('<option value="'+editList[i][0]+'" id="entity-value-1" class="fas" data-toggle="tooltip" data-container="#tooltip_container">&#xf05a;&nbsp'+editList[i].slice(1,editList[i].length-1).join()+'</option>'); //Changed by Heet
    //       }
    //       else{

    //            jQuery("#node-edit-entity-options").append('<option value="'+editList[i][0]+'" id="entity-value-1" class="fas" >'+editList[i].slice(1,editList[i].length-1).join()+'</option>'); //Changed by Heet
    //        }
    //    };
    //    jQuery('#tooltip_container').remove();
    //    jQuery('<div id="tooltip_container"></div>').insertAfter("#node-edit-entity-options");
    //      if(nodeEditWindowNo == 0){
    //         jQuery("#node-edit-entity-back").attr("disabled",true);}
    //     //     jQuery('#loading-indicator').hide();
    //     //     jQuery('#loading').modal('hide');
    //     // },1000);



});

jQuery("#result-back").click(function() {
    if(queryWindowNum > 1) {
        queryWindowNum--;
        updateQueryResultTable(false);
    }
});

jQuery("#result-next").click(function() {
    queryWindowNum++;
    updateQueryResultTable(false);
});


jQuery("#node-edit-entity-next").click(function() {
    event.stopPropagation();
    nodeEditWindowNo++;
    var typeVal = jQuery("#edit-type-selected-value").val();
    var domainVal = jQuery("#edit-domain-selected-value").val();
    if (!typeVal) {
        typeVal = -1;
    }
    if (!domainVal) {
        domainVal = -1;
    }
    loadEditEntityList(domainVal, typeVal);
    jQuery("#node-edit-entity-back").attr("disabled", false);

    //     // jQuery('#loading').modal('show');
    //     // jQuery('#loading-indicator').show();
    //     // setTimeout(function(){
    //       nodeEditWindowNo++;
    //       if (nodeEditWindowNo != 0){
    //           jQuery("#node-edit-entity-back").attr("disabled",false);}
    //       editList.length = 0;
    //       var text = jQuery("#node-edit-type-options option:selected").text();
    //     value = jQuery("#node-edit-type-options option:selected").val();
    //     if( text.toLowerCase() != "select type..." && text != ""){
    //       getRequest("https://" + urlFull + "/orion_app/geteditnode?node="+value+"&windownum="+nodeEditWindowNo+"&windowsize="+windowSize+"&edges=&keyword="+editNodeKeyword, editList);
    //     }
    //     else{
    //       getRequest("https://" + urlFull + "/orion_app/geteditnode?node="+nodeToBeEdited.nodeID+"&windownum="+nodeEditWindowNo+"&windowsize="+windowSize+"&edges="+nodeEditEdges+"&keyword="+editNodeKeyword, editList);
    //     }


    //       if(editList.length < 19){
    //           jQuery("#node-edit-entities-options").attr('size',editList.length);}
    //       else{
    //           jQuery("#node-edit-entity-options").attr('size',19);
    //       }
    //       if (editList.length <= windowSize){
    //          jQuery("#node-edit-entity-next").attr("disabled", true);}
    //       jQuery("#node-edit-entity-options").empty();
    //       for (var i = 0; i < editList.length; i++) {
    //           if(editList[i][editList[i].length-1]=="preview"){
    //                jQuery("#node-edit-entity-options").append('<option value="'+editList[i][0]+'" id="entity-value-1" class="fas" data-toggle="tooltip" data-container="#tooltip_container">&#xf05a;&nbsp'+editList[i].slice(1,editList[i].length-1).join()+'</option>'); //Changed by Heet
    //           }
    //           else{

    //                jQuery("#node-edit-entity-options").append('<option value="'+editList[i][0]+'" id="entity-value-1" class="fas" >'+editList[i].slice(1,editList[i].length-1).join()+'</option>'); //Changed by Heet
    //            }
    // //           jQuery("#node-edit-entity-options").append('<option value="'+editList[i][0]+'" id="entity-value-1">'+editList[i][1]+'</option>');
    //       };
    //     //   jQuery('#loading-indicator').hide();
    //     //   jQuery('#loading').modal('hide');
    //     // },1000);

});

jQuery("#node-edit-entity-options").on('click', function() {
    var text = jQuery("#node-edit-entity-options option:selected").text();
    var value = jQuery("#node-edit-entity-options option:selected").val();
    if(value == prevEntSel) {
      deselectEditEntity();
      prevEntSel = null;
      return;
    }
    prevEntSel = value;
    if (text != "Select Entity..." && text != "") {
        if (text[0] == "") {
            text = text.substring(2);
        }
        jQuery("#edit-entity-selected-value").attr("value", text);
        jQuery("#edit-entity-selected").attr("value", text);
        // jQuery("#edit-entity-selected").show();
        // jQuery("#edit-entity-x-button").show();

    }

    toggleConfirmButton("node-edit-");
});


jQuery("#edge-options").on('change', function() {
    var text = jQuery("#edge-options option:selected").text();
    if (text.toLowerCase() != " select edge" && text != "") {
        jQuery("#save-changes").css("background-color", "#2ea2cc");
    } else {

        jQuery("#save-changes").css("background-color", "#828b8f");
    }
});



jQuery("#node-edit-type-options").on('click', function() {


    var text = jQuery("#node-edit-type-options option:selected").text();
    var value = jQuery("#node-edit-type-options option:selected").val();
    if(value == prevTypSel) {
      deselectEditType();
      prevTypSel = null;
      return;
    }
    prevTypSel = value;
    var domainName = getDomainForType(value);
    var typeExists = false;
    for(var elem of curTypeList.concat(curEndTypeList)) {
        if(elem.split(':')[0] == value) {
            typeExists = true;
        }
    }
    if(typeExists == false) {
      jQuery("#edit-add-type-text").html("Require this node to be in Type <b>"+text.toUpperCase()+"</b> (in Domain <i><b>"+domainName[1].toUpperCase()+"</b></i>)?").show();
      jQuery("#edit-add-type-button").show();
    }
    else {
      jQuery("#edit-add-type-text").hide();
      jQuery("#edit-add-type-button").hide();
    }
    if (text.toLowerCase() != "select type..." && text != "") {
        jQuery("#edit-type-selected-value").attr('value', value);
        jQuery("#edit-type-selected").attr("value", text);
        // jQuery("#edit-type-selected").show();
        // jQuery("#edit-type-x-button").show();
        jQuery("#edit-entity-selected").hide();
        jQuery("#edit-entity-x-button").hide();
        nodeEditWindowNo = 0;
        jQuery.ajax({
            type: "GET",
            beforeSend: function(request) {
                request.setRequestHeader("Content-type", "application/json");
            },
            //          url:"https://" + urlFull + "/orion_app/geteditnode?node="+value+"&windownum="+nodeEditWindowNo+"&windowsize="+windowSize+"&edges=&keyword=",
            url: "https://" + urlFull + "/orion_app/getentities?domain=-1&windownum=0&windowsize=100&type=" + value + "&keyword=",
            processData: false,
            dataType: "json",
            async: true,
            success: function(data) {
                nodeEditList.length = 0;
                jQuery("#node-edit-entity-options").empty();
                for (var i = 0; i < data.length; i++) {
                    var temp = data[i].split(",");
                    nodeEditList[i] = temp;
                };
                for (var i = 0; i < nodeEditList.length; i++) {
                    if (nodeEditList[i][nodeEditList[i].length - 1] == "preview") {
                        jQuery("#node-edit-entity-options").append('<option value="' + nodeEditList[i][0] + '" id="add-value-1" data-html="true" data-toggle="tooltip" data-container="#tooltip_container">' + nodeEditList[i].slice(1, nodeEditList[i].length - 1).join() + '</option>'); //Changed by Heet
                    } else {

                        jQuery("#node-edit-entity-options").append('<option value="' + nodeEditList[i][0] + '" id="add-value-1"  >' + nodeEditList[i].slice(1, nodeEditList[i].length - 1).join() + '</option>'); //Changed by Heet
                    }
                };
                jQuery('#tooltip_container').remove();
                jQuery('<div id="tooltip_container"></div>').insertAfter("#node-edit-entity-options");
                jQuery("#node-edit-entity").show();
                jQuery("#node-edit-entity-search").show();
            },
            error: function() {
                nodeEditList.length = 0;
                data = ["123|Select Name", "234|Domain1", "4345|Domain2", "141|domain3", "3455|Domain4", "2134|Domain5", "7575|Domain6", "7575s|Domain7", "73275|Domain8", "75115|Domain9"];
                for (var i = 0; i < data.length; i++) {
                    var temp = data[i].split("|");
                    nodeEditList[i] = temp;
                }

            }
        });
    }

    toggleConfirmButton("node-edit-");
    // var ot = jQuery('#node-edit-domain-options').find('[value="' + domainName[0] + '"]').offset().top;
    // var st = jQuery('#node-edit-domain-options').offset().top;
    // jQuery('#node-edit-domain-options').scrollTop(jQuery('#node-edit-domain-options').scrollTop() + ot - st);
    // jQuery('#node-edit-domain-options').find('[value="' + domainName[0] + '"]').prop('selected', true);
    // jQuery("#edit-domain-selected").attr('value', domainName[1]);
    // jQuery("#edit-domain-selected").attr('title', domainName[1]);
    // if (text != "Select Domain...") {
    //     jQuery("#edit-domain-selected").show();
    //     jQuery("#edit-domain-x-button").show();
    // }



});
jQuery("#node-edit-save-changes").click(function() {
    var text;
    var value;
    if (jQuery("#node-edit-save-changes").css("background-color") == "rgb(130, 139, 143)") {
        return;
    }
    nodeToBeEdited.typeList = curTypeList.slice();
    text = jQuery("#node-edit-type-options option:selected").text().toLowerCase();
    value = jQuery("#node-edit-type-options option:selected").val();
    if(value != -1 && value != undefined) {
      var selectedType = value+":"+getDomainForType(value)[1]+":"+text;

      if(nodeToBeEdited.typeList.includes(selectedType) == false) {
        nodeToBeEdited.typeList.push(selectedType);
      }
    }

    if (nodeToBeEdited.entity == -1) {
        text = jQuery("#node-edit-entity-options option:selected").text();
        if (text[0] == "") {
            text = text.substring(2);
        }
        value = jQuery("#node-edit-entity-options option:selected").val();
        if (text != "Select Entity..." && text != "") {
            for (var k = 0; k < nodes.length; k++) {
                if (nodeToBeEdited === nodes[k]) {
                    nodes[k].entity = value;
                    nodes[k].nodeID = value;
                    nodes[k].name = text;
                    nodes[k].tempName = text;
                }
            }
        } else {
            // text = jQuery("#node-edit-type-options option:selected").text();
            // value = jQuery("#node-edit-type-options option:selected").val();

            for (var k = 0; k < nodes.length; k++) {
                if (nodeToBeEdited === nodes[k]) {

                    nodes[k].nodeID = parseInt(nodeToBeEdited.typeList.concat(curEndTypeList)[0].split(':')[0]);

                    var typeLabels = [];
                    //only adding user added types to the label, because restart() will add the edge types later
                    for(var elem of nodeToBeEdited.typeList) {
                      typeLabels.push(elem.split(':')[2]);
                    }
                    nodes[k].name = typeLabels.join(' & ');
                    nodes[k].tempName = typeLabels.join(' & ');
                    nodes[k].entity = -1;
                }
            }

            for (var k = 0; k < links.length; k++) {
                if (links[k].source != links[k].target && links[k].flag == 2) {
                    if (links[k].source == nodeToBeEdited) {
                        links[k].actualSourceType = parseInt(value);

                    }
                    if (links[k].target == nodeToBeEdited) {
                        links[k].actualTargetType = parseInt(value);
                    }
                }
            }
        }
    } else {
        text = jQuery("#node-edit-entity-options option:selected").text();
        if (text[0] == "") {
            text = text.substring(2);
        }
        value = jQuery("#node-edit-entity-options option:selected").val();
        if (text != "Select Entity..." && text != "") {
            for (var k = 0; k < nodes.length; k++) {
                if (nodeToBeEdited === nodes[k]) {
                    nodes[k].entity = value;
                    nodes[k].nodeID = value;
                    nodes[k].name = text;
                    nodes[k].tempName = text;
                }
            }
        } else {
            // text = jQuery("#node-edit-type-options option:selected").text();
            // value = jQuery("#node-edit-type-options option:selected").val();
            // if (text.toLowerCase() != "select type..." && text != "") {
                for (var k = 0; k < nodes.length; k++) {
                    if (nodeToBeEdited === nodes[k]) {

                        nodes[k].nodeID = parseInt(nodeToBeEdited.typeList.concat(curEndTypeList)[0].split(':')[0]);

                        var typeLabels = [];
                        //only adding user added types to the label, because restart() will add the edge types later
                        for(var elem of nodeToBeEdited.typeList) {
                          typeLabels.push(elem.split(':')[2]);
                        }
                        nodes[k].name = typeLabels.join(' & ');
                        nodes[k].tempName = typeLabels.join(' & ');
                        //nodes[k].tempName = text;
                        nodes[k].entity = -1;
                    }
                }

                for (var k = 0; k < links.length; k++) {
                    if (links[k].source != links[k].target && links[k].flag == 2) {
                        if (links[k].source == nodeToBeEdited) {
                            links[k].actualSourceType = parseInt(value);

                        }
                        if (links[k].target == nodeToBeEdited) {
                            links[k].actualTargetType = parseInt(value);
                        }
                    }
                }
            //}


        }

    }
    jQuery("#node-edit-border").hide();
    jQuery("#node-edit-entity").hide();
    jQuery("#node-edit-entity-search").hide();
    jQuery("#node-edit-type").hide();
    jQuery("#nodeEditModal").modal('hide');
    nodeToBeEdited = null;
    checkKeywordStatus();

    restart();
    refreshAllSuggestion();
});


function deselectEditDomain() {
    loadEditTypeList();
    loadEditEntityList();
    jQuery("#node-edit-domain-options option:selected").attr('selected', false);
    jQuery("#edit-domain-selected").hide();
    jQuery("#edit-domain-x-button").hide();
    jQuery("#edit-type-selected").hide();
    jQuery("#edit-type-x-button").hide();
    jQuery("#edit-entity-selected").hide();
    jQuery("#edit-entity-x-button").hide();
    toggleConfirmButton("node-edit-");
}

function deselectEditType() {
    jQuery("#edit-add-type-text").hide();
    jQuery("#edit-add-type-button").hide();
    jQuery("#node-edit-type-options option:selected").attr('selected', false);
    loadEditEntityList();
    jQuery("#edit-type-selected").hide();
    jQuery("#edit-type-x-button").hide();
    jQuery("#edit-entity-selected").hide();
    jQuery("#edit-entity-x-button").hide();
    toggleConfirmButton("node-edit-");
}


function deselectEditEntity() {
    jQuery("#node-edit-entity-options option:selected").attr('selected', false);
    jQuery("#edit-entity-selected").hide();
    jQuery("#edit-entity-x-button").hide();
    toggleConfirmButton("node-edit-");
}


jQuery("#node-edit-domain-options").on('click', function() {

    var text = jQuery("#node-edit-domain-options option:selected").text();
    var value = jQuery("#node-edit-domain-options option:selected").val();
    if(value == prevDomSel) {
      deselectEditDomain();
      prevDomSel = null;
      return;
    }
    prevDomSel = value;

    jQuery("#edit-domain-selected-value").attr('value', value);
    jQuery("#edit-domain-selected").attr("value", text);
    // jQuery("#edit-domain-selected").show();
    // jQuery("#edit-domain-x-button").show();
    jQuery("#edit-type-selected").hide();
    jQuery("#edit-type-x-button").hide();
    jQuery("#edit-entity-selected").hide();
    jQuery("#edit-entity-x-button").hide();

    jQuery("#edit-add-type-text").hide();
    jQuery("#edit-add-type-button").hide();
    loadEditTypeList(value);
    loadEditEntityList(domainVal = value, typeVal = jQuery("#edit-type-selected-value").val());
    toggleConfirmButton("node-edit-");
});


function displayAllTheTypeOptions() {
    displayTypeOptions(types);
}

function displayAllTheEntityOptions() {
    displayEntityOptions(entities);
}

function mouseup() {
    if (waitForJsonResultFlag == 0) {
        allowNodeCreation = true;
        if (mousedown_node) {
            // hide drag line
            drag_line
                .classed('hidden', true)
                .style('marker-end', '');
        }

        // because :active only works in WebKit?
        svg.classed('active', false);

        // clear mouse event vars
        resetMouseVars();
        // removeTempNodes();
        // if (selected_node && nodeclick==false) {
        //   selected_node=null;
        // };

        if (selected_link && link_click == false) {
            if (selected_link.flag != 2) {
                if (selected_link.flag == 3) {
                    jQuery("#linkId_" + selected_link.id).css('stroke', '#B0B0B0');
                } else if (selected_link.flag == 4) {
                    jQuery("#linkId_" + selected_link.id).css('stroke', '#2EFEF7');
                }
            } else {
                jQuery("#linkId_" + selected_link.id).css('stroke', 'black');
            }
            //     selected_link = null;
        };
        link_click = false;
        nodeclick = false;
        animatedHelp();
        restart();
    }
}

// var createNewNodeModal = getElementById("#myModal");
// window.onclick = function (event){
//   if(event.target == createNewNodeModal){
//     createNewNodeModal.style.display = "none";
//   }
// }

function removeTempNodes() {
    if (suggestions == true && nodeclick == false) {
        for (var k = 0; k < links.length; k++) {
            if (links[k].flag == 4 && links[k].source != links[k].target) {
                updateRejectedGraphForSelectedGreyLink(links[k]);
            }
        }
        for (var i = 0; i < nodes.length; i++) {
            if (nodes[i].flag == 1 && nodes[i].greyflag == 2) {
                removeAllInstancesOfEdge(nodes[i]);
            }
            if (nodes[i].flag == 2) {
                removeAllInstancesOfEdge(nodes[i]);
                spliceLinksForNode(nodes[i]);
                nodes.splice(nodes.indexOf(nodes[i]), 1);
                i--;
            };

        };
        suggestions = false;
        // createNewNode();
        // suggestionPicked = false;
        // addSuggestions();
    }
    for (var n = 0; n < links.length; n++) {
        if (links[n].flag == 3 && links[n].source != links[n].target) {
            links[n].source = links[n].target;
        }
        if (links[n].flag == 20) {
            links[n].source = links[n].target;
        }
    }
    selected_node = null;
    //   if (notnull != true){
    selected_link = null;
    //  notnull = false;
    //   }
}


function updateRejectedGraphForSelectedGreyLink(link) {
    var linkFound = 0;
    var entry = allReturnObject[0];
    for (var i = 0; i < allReturnObject.length; i++) {
        if (link.linkID == entry.edge.split('|')[0] && link.source.id == entry.graphSource && link.target.id == entry.graphObject && link.source.nodeID == entry.source.split('|')[0] && link.target.nodeID == entry.object.split('|')[0]) {
            linkFound = 1;
        } else if (link.linkID == entry.edge.split('|')[0]) {
            rejectedGraph.push({
                source: entry.source.split('|')[0],
                graphSource: entry.graphSource,
                edge: link.linkID,
                object: entry.object.split('|')[0],
                graphObject: entry.graphObject
            });
            linkFound = 1;
        }
        if (i <= (allReturnObject.length - 2)) {
            var prevLinkId = entry.edge.split('|')[0];
            entry = allReturnObject[i + 1];
            if (prevLinkId != entry.edge.split('|')[0] && linkFound == 1) {
                break;
            }
        }
    }
}



function removeAllInstancesOfEdge(node) {
    if (suggestionPicked || nextButtonClick) {
        var spliceAll = links.filter(function(l) {
            return (l.source === node || l.target === node);
        });
        spliceAll.map(function(l) {
            var linkFound = 0;
            var entry = allReturnObject[0];
            var idSource;
            var idTarget;
            if (l.source.greyflag == 0 && l.target.greyflag == 0) {
                idSource = l.source.id;
                idTarget = l.target.id;
            } else if (l.source.greyflag == 0) {
                idSource = l.source.id;
                idTarget = -1;
            } else {
                idSource = -1;
                idTarget = l.target.id;
            }

            for (var i = 0; i < allReturnObject.length; i++) {
                if (l.linkID == entry.edge.split('|')[0] && idSource == entry.graphSource && idTarget == entry.graphObject && l.source.nodeID == entry.source.split('|')[0] && l.target.nodeID == entry.object.split('|')[0]) {
                    linkFound = 1;
                } else if (l.linkID == entry.edge.split('|')[0]) {
                    rejectedGraph.push({
                        source: entry.source.split('|')[0],
                        graphSource: entry.graphSource,
                        edge: l.linkID,
                        object: entry.object.split('|')[0],
                        graphObject: entry.graphObject
                    });
                    linkFound = 1;
                }
                if (i <= (allReturnObject.length - 2)) {
                    var prevLinkId = entry.edge.split('|')[0];
                    entry = allReturnObject[i + 1];
                    if (prevLinkId != entry.edge.split('|')[0] && linkFound == 1) {
                        break;
                    }
                }
            }
        });
    }
}



function spliceLinksForNode(node) {
    var toSplice = links.filter(function(l) {
        return (l.source === node || l.target === node);
    });
    toSplice.map(function(l) {
        if (suggestionPicked || nextButtonClick) {
            var graphSrcId;
            var graphTargetId;
            if (l.source.greyflag == 0 && l.target.greyflag == 0) {
                graphSrcId = l.source.id;
                graphTargetId = l.target.id;
            } else if (l.source.greyflag == 0) {
                graphSrcId = l.source.id;
                graphTargetId = -1;
            } else {
                graphSrcId = -1;
                graphTargetId = l.target.id;
            }
            rejectedGraph.push({
                source: l.source.nodeID,
                graphSource: graphSrcId,
                edge: l.linkID,
                object: l.target.nodeID,
                graphObject: graphTargetId
            });
        }
        l.source = l.target;
    });
}

// only respond once per keydown
var lastKeyDown = -1;

function keydown() {
    // console.log("keyCode: "+ d3.event.keyCode);
    //d3.event.preventDefault();
    //Edited By Heet
    if (jQuery(event.target).hasClass('textboxlist-bit-editable-input'))
        return;


    if (d3.event.keyCode === 8) {
        if (event.target == document.body) {
            // stop this event from propagating further which prevents
            // the browser from doing the 'back' action
            d3.event.preventDefault();
        }
    }

    //    else if (d3.event.keyCode == 13 || d3.event.keyCode == 16) {
    //     drawEdge = true;
    //     allowNodeDrag = false;
    //     translateAllowed = false;
    //     allowNodeCreation = true;
    //   }
    else if (d3.event.keyCode == 46) {
        if (selected_node) {
            for (var x = 0; x < links.length; x++) {
                if (links[x].source === selected_node && (links[x].target.greyflag == 1 || links[x].target.greyflag == 2)) {
                    nodes.splice(nodes.indexOf(links[x].target), 1);
                } else if (links[x].target === selected_node && (links[x].source.greyflag == 1 || links[x].source.greyflag == 2)) {
                    nodes.splice(nodes.indexOf(links[x].source), 1);
                }
            }
            nodes.splice(nodes.indexOf(selected_node), 1);
            spliceLinksForNode(selected_node);
            var graphDisconnect = checkConnected(nodes, links);
            if (graphDisconnect == true) {
                //             jQuery("#button-g").hide();
            } else {
                jQuery("#submit-button").attr("class", "menu-border");
                jQuery("#button-g").attr("class", "menu-border");
            }
        } else if (selected_link) {
            jQuery("#edge" + selected_link.id.toString()).remove();
            jQuery("#linkId_" + selected_link.id.toString()).remove();
            jQuery("#linkLabelHolderId_" + selected_link.id.toString()).remove();
            selected_link.source = selected_link.target;
            if (edgeTypesFlag == 1) {
                jQuery("#wrap4").toggleClass('active');
                jQuery("#edge-types").toggleClass('active');
                edgeTypesFlag = 0;
            }
            if (exampleReverseRoleFlag == true) {
                exampleSource = null;
                exampleTarget = null;
                exampleLinkId = -1;
                exampleLinkGraphId = -1;
                exampleReverseRoleFlag = false;
                exampleActualSourceType = null;
                exampleActualTargetType = null;
                addEdgeButtonEdgeAdded = false;
                jQuery("#Add-Edge-button").hide();
                jQuery("#example-text").show();
                jQuery("#active-examples").empty();
                jQuery("#Reverse-button").hide();
            }
            // links.splice(links.indexOf(selected_link), 1);
            var graphDisconnect = checkConnected(nodes, links);
            if (graphDisconnect == true) {
                //             jQuery("#button-g").hide();
            } else {
                jQuery("#submit-button").attr("class", "menu-border");
                jQuery("#button-g").attr("class", "menu-border");
            }
        }
        selected_link = null;
        selected_node = null;
        animatedHelp();
        restart();
    } else {
        // if(d3.event.target ==



    }
}

function keyup() {
    if (d3.event.keyCode == 13 || d3.event.keyCode == 16) {
        //     allowNodeDrag = true;
        //     translateAllowed = true;
        //     drawEdge = false;
        //     allowNodeCreation = false;
    }
}

zoom.on("zoom", function() {
    // d3.select("#"+gID).attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
    if (translateAllowed == true && nodeclick == false)
        g.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
});
// svg.call(zoom);

function dragstart(d) {
    force.start();
    if (translateAllowed && nodeclick) {
        //     force.stop();
        d.fixed = true;
        // d3.select(this).classed("fixed", d.fixed = true);
        // console.log("node drag");
    };
}

function dragmove(d) {
    if (translateAllowed == false) {
        force.start();
    }

    if (translateAllowed && nodeclick) {
        d.x += d3.event.dx;
        d.y += d3.event.dy;
        d.px += d3.event.dx;
        d.py += d3.event.dy;
        //     if(d!=null)
        //       tick();
        // restart();
    }
    if (translateAllowed == false) {
        force.stop();
    }

}

function dragend(d) {
    if (translateAllowed && nodeclick) {
        // d.fixed = true;
        //     if(d!=null)
        //     tick();
        //     force.start();
    }
}

function temp() {
    console.log("mouse move");
}

function nextClick(mode) {
    if (exampleReverseRoleFlag == true) {
        jQuery("#linkId_" + exampleLinkGraphId).css('stroke', 'black');
        exampleSource = null;
        exampleTarget = null;
        exampleLinkId = -1;
        exampleLinkGraphId = -1;
        exampleReverseRoleFlag = false;
        animatedHelp();
        exampleActualSourceType = null;
        exampleActualTargetType = null;
        addEdgeButtonEdgeAdded = false;
        jQuery("#example-text").show();
        jQuery("#Add-Edge-button").hide();
        jQuery("#active-examples").empty();
        jQuery("#Reverse-button").hide();
    }
    nextButtonClick = true;
    if (selected_node != null) {
        if (selected_node.nodeID != -1) {
            refreshSelectedNode = selected_node.id;
        } else {
            refreshSelectedNode = 0;
        }
    } else {
        refreshSelectedNode = 0;
    }
    removeTempNodes();
    nextButtonClick = false;
    restart();
    var singleNode = 0;
    if (nodes.length == 1) {
        singleNode = 1;
    }
    addSuggestions(singleNode, mode);
}

function addSuggestions(singleNode, mode) {

    var modeValue = 1;
    var refreshGraphNode = 0;
    if (mode == 1) {
        if (refreshSelectedNode != 0) {
            modeValue = 2;
        }
        refreshGraphNode = refreshSelectedNode;
    }
    //   else {
    //     modeValue=2;
    //   }
    if (nodes.length > 0) {
        jQuery("#submit-button").attr("class", "menu-border");
        jQuery("#button-g").attr("class", "menu-border");
    }
    if (singleNode == 1) {
        // The passive edge suggestion call made after the very first node is added. The value of edge and object node is -1 and 0
        // respectively.
        partialGraph = [];
        partialGraph[0] = {
            source: nodes[0].nodeID,
            graphSource: nodes[0].id,
            edge: -1,
            object: 0,
            graphObject: -1,
            sourceTypeValues: getTypeValues(nodes[0].typeList)+","+getTypeValues(findEdgeEndTypes(nodes[0])),
            objectTypeValues: "",
            sourceEntity: nodes[0].entity,
            objectEntity: -1
        };
    } else {
        partialGraph = [];
        var endTypeValues = new Map();
        for (var i = 0, j = 0; i < links.length; i++) {
            if (links[i].linkID != -1 && (links[i].source != links[i].target) && (links[i].source.greyflag == 0 || links[i].target.greyflag == 0)) {
                if(!endTypeValues.has(links[i].source)) endTypeValues.set(links[i].source, findEdgeEndTypes(links[i].source));
                if(!endTypeValues.has(links[i].target)) endTypeValues.set(links[i].target, findEdgeEndTypes(links[i].target));
                partialGraph[j] = {
                    source: links[i].source.nodeID,
                    graphSource: links[i].source.id,
                    edge: links[i].linkID,
                    object: links[i].target.nodeID,
                    graphObject: links[i].target.id,
                    sourceTypeValues: getTypeValues(links[i].source.typeList)+","+getTypeValues(endTypeValues.get(links[i].source)),
                    objectTypeValues: getTypeValues(links[i].target.typeList)+","+getTypeValues(endTypeValues.get(links[i].target)),
                    sourceEntity: links[i].source.entity,
                    objectEntity: links[i].target.entity
                };
                j++;
            }
        };
        for (var l = 0, k = partialGraph.length; l < nodes.length; l++) {
            var checklink = false;
            for (var i = 0; i < links.length; i++) {
                if ((nodes[l] == links[i].source || nodes[l] == links[i].target) && links[i].source != links[i].target) {
                    checklink = true;
                }
            }
            if (!checklink) {
                partialGraph[k] = {
                    source: nodes[l].nodeID,
                    graphSource: nodes[l].id,
                    edge: -1,
                    object: 0,
                    graphObject: -1,
                    sourceTypeValues: getTypeValues(nodes[l].typeList)+","+getTypeValues(findEdgeEndTypes(nodes[l])),
                    objectTypeValues: "",
                    sourceEntity: nodes[l].entity,
                    objectEntity: -1
                };
                k++;
            }
        }
    }


    var data = {
        partialGraph: partialGraph,
        nodes: nodes,
        mode: modeValue,
        rejectedGraph: rejectedGraph,
        dataGraphInUse: 0,
        topk: noOfSuggestions,
        refreshGraphNode: refreshGraphNode
    };
    refreshSelectedNode = 0;
    suggestion_mode = 0;

    // if (suggestionCounter == 0) {
    postRequest(data, mode);
}




function checkLinks(link) {
    for (var i = 0; i < links.length; i++) {
        if ((links[i].source.name == link.source.name || links[i].source.name == link.target.name) && (links[i].target.name == link.target.name || links[i].target.name == link.source.name)) {
            return false;
        }
    }
    return true;
}

/*
Functions controls the active help.
adds suggestions based on certain conditions.
*/
function animatedHelp() {
    // jQuery("#wrap1").toggleClass('active');
    //jQuery("#possible-actions").toggleClass('active');
    if (nodes.length == 0 && links.length == 0) {
        jQuery("#imgPossibleActions").attr('src', 'images/case1.PNG');
        jQuery("#active-help").empty();
        jQuery("#active-help").append('<li class="li-animation" style="padding-top:10px;"><b>Click</b> on the canvas to add a new node.</li>');
    } else if (checkConnected(nodes, links) && !suggestions) {
        jQuery("#imgPossibleActions").attr('src', 'images/case2.PNG');
        jQuery("#active-help").empty();
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Add</b> an edge between the newly added node and an existing node in the graph, by holding down the Shift key, clicking on one node and dragging the mouse pointer to the other.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Delete</b> the unconnected node.</li>');
    } else if (suggestions && !suggestionPicked && subSuggestionsInProgressFlag == false && exampleReverseRoleFlag == false) {
        jQuery("#imgPossibleActions").attr('src', 'images/case3.PNG');
        jQuery("#active-help").empty();
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on a grey node to add it and its incident edge to the query graph.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on the grey edge to select it and its corresponding grey node, or display the other occurrences of the grey edge, if any.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on the empty canvas to ignore the suggestions and add a new node.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on Try Different Suggestions to get new edge suggestions.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on an orange node, and then click on Try Different Suggestions to get new edge suggestions incident on the orange node.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on the empty canvas, close the new node dialog box, and click on a black edge to view its source and destination types.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on the empty canvas, close the new node dialog box, and double click on an orange node to edit its value.</li>');
    } else if (suggestions && suggestionPicked && subSuggestionsInProgressFlag == false && exampleReverseRoleFlag == false) {
        jQuery("#imgPossibleActions").attr('src', 'images/case4.PNG');
        jQuery("#active-help").empty();
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on other grey nodes to be included in the query graph.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on the grey edge to select it, or click on a grey edge to display the other occurrences of the grey edgei, if any.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on the empty canvas to add the selected nodes and edges to the query graph while ignoring the unselected grey nodes, and display new suggestions.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on selected nodes (in blue) to unselect them.</li>');
    } else if (suggestions && SugPickdPerGreyLinkCount == 0 && greyLinkPickdPerSubSuggestion == 0 && subSuggestionsInProgressFlag == true) {
        jQuery("#imgPossibleActions").attr('src', 'images/case5.PNG');
        jQuery("#imgPossibleActions").attr('alt', 'case5');
        jQuery("#active-help").empty();
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on a grey node to add it and its incident edge to the query graph.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on a grey edge to add it to the query graph, or on a grey node to add the node and its incident edge to the query graph.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on the empty canvas to ignore the various unselected occurrences nodes and edges in grey, and go back to original set of suggestions.</li>');
    } else if (suggestions && (SugPickdPerGreyLinkCount > 0 || greyLinkPickdPerSubSuggestion > 0) && subSuggestionsInProgressFlag == true) {
        jQuery("#imgPossibleActions").attr('src', 'images/case6.PNG');
        jQuery("#imgPossibleActions").attr('alt', 'case6');
        jQuery("#active-help").empty();
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on other grey nodes to be included in the query graph.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on a grey edge to add it to the query graph, or on a grey node to add the node and its incident edge to the query graph.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on the empty canvas to display original unselected suggestions in grey and the selected suggestions in blue.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on selected nodes to unselect them. Click on selected edge (in blue) between two orange nodes to unselect it.</li>');
    } else if ((selectNodeModalClosed == true && !suggestions) || (!suggestions && exampleReverseRoleFlag == false && subSuggestionsInProgressFlag == false)) {
        jQuery("#imgPossibleActions").attr('src', 'images/case7.PNG');
        jQuery("#imgPossibleActions").attr('alt', 'case7');
        selectNodeModalClosed = false;
        jQuery("#active-help").empty();
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Double</b> click on an orange node to edit its value.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on an edge to view its source type, object type.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on Try Different Suggestions to get new edge suggestions.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on an orange node, and then click on Try Different Suggestions to get new edge suggestions incident on the orange node.</li>');
    } else if (exampleReverseRoleFlag == true && !suggestions && addEdgeButtonEdgeAdded == false) {
        jQuery("#imgPossibleActions").attr('src', 'images/case8.PNG');
        jQuery("#imgPossibleActions").attr('alt', 'case8');
        jQuery("#active-help").empty();
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on Add Edge button to add other instances of the selected edge, connected to the nodes at the two ends of the selected edge.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on Reverse Role button to reverse the role of source and destination of the edge.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on the selected edge to remove souce type and destination type of edge.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on Try Different Suggestions to remove source type and destination type of edge and display new suggestions.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on an orange node, and then click on Try Different Suggestions to remove source type and destination type of edge and display new suggestions incident on the selected orange node.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on empty canvas to remove source type and destination type of edge.</li>');
    } else if (suggestions && !suggestionPicked && exampleReverseRoleFlag == true && addEdgeButtonEdgeAdded == true) {
        jQuery("#imgPossibleActions").attr('src', 'images/case9.PNG');
        jQuery("#imgPossibleActions").attr('alt', 'case9');
        jQuery("#active-help").empty();
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on a grey node to add it and its incident edge to the query graph.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on the empty canvas to ignore the automatic edge suggestions and add a new node.</li>');
    } else if (suggestions && suggestionPicked && exampleReverseRoleFlag == true && addEdgeButtonEdgeAdded == true) {
        jQuery("#imgPossibleActions").attr('src', 'images/case10.PNG');
        jQuery("#imgPossibleActions").attr('alt', 'case10');
        jQuery("#active-help").empty();
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on other grey nodes to be included in the query graph.</li>');
        jQuery("#active-help").append('<li class="li-animation" style="padding-top: 10px;"><b>Click</b> on the empty canvas to add the selected nodes and edges to the query graph while ignoring the unselected grey nodes, and display new suggestions.</li>');
    }
    jQuery('#active-help').each(function() {
        // jQuery(this).children().each(function(i) {
        //     jQuery(this).delay((i++) * 1500).animate({left:0, opacity:1});
        // });
        jQuery(this).children().animate({
            left: 0,
            opacity: 1
        });
    });
    jQuery("#wrap1").toggleClass('active');
    jQuery("#possible-actions").toggleClass('active');
}


function getGraphStringUserStudy(graph, isPartialGraph) {
    var str = "";
    var i = 0;
    for (i = 0; i < graph.length; i++) {
        if(isPartialGraph) {
            //var src = Array.from(new Set(graph[i].sourceTypeValues.split(","))).filter(x => x !== "").join(",");
            var src = new Set(graph[i].sourceTypeValues.split(","));
            src.add(graph[i].source.toString());
            src = Array.from(src).filter(x => x !== "").join(",");
            
            //var obj = Array.from(new Set(graph[i].objectTypeValues.split(","))).filter(x => x !== "").join(",");
            var obj = new Set(graph[i].objectTypeValues.split(","));
            obj.add(graph[i].object.toString());
            obj = Array.from(obj).filter(x => x !== "").join(",");
            str += src + "," + graph[i].edge + "," + obj + ";";
        } else {
            str += graph[i].source + "," + graph[i].edge + "," + graph[i].object + ";";
        }
    }
    return str;
}

function getGraphString(graph) {
    var src = "\"source\":";
    var graphSrc = "\"graphSource\":";
    var prop = "\"edge\":";
    var obj = "\"object\":";
    var graphObj = "\"graphObject\":";
    var srcType = "\"sourceTypeValues\":";
    var objType = "\"objectTypeValues\":";
    var srcEntity = "\"sourceEntity\":";
    var objEntity = "\"objectEntity\":";
    var str = "[";
    for (var i = 0; i < graph.length; i++) {
        str += "{" + src + graph[i].source + ",";
        str += graphSrc + graph[i].graphSource + ",";
        str += prop + graph[i].edge + ",";
        str += graphObj + graph[i].graphObject + ",";
        str += obj + graph[i].object + ",";
        str += srcType + "\""+ graph[i].sourceTypeValues+ "\"" + ",";
        str += objType + "\""+ graph[i].objectTypeValues+ "\"" + ",";
        str += srcEntity + "\""+ graph[i].sourceEntity+ "\"" + ",";
        if (i == (graph.length-1)) {
          str += objEntity+ "\"" + graph[i].objectEntity+ "\"" + "}";
        } else {
          str += objEntity+ "\"" + graph[i].objectEntity+ "\"" + "},";
        }
    }
    // if (graph.length > 0) {
    //     str += "{" + src + graph[i].source + ",";
    //     str += graphSrc + graph[i].graphSource + ",";
    //     str += prop + graph[i].edge + ",";
    //     str += graphObj + graph[i].graphObject + ",";
    //     str += obj + graph[i].object + "}";
    // }
    str += "]";
    return str;
}

function myStringify(data) {
    var jsonstr = "{";
    jsonstr += "\"partialGraph\":" + getGraphString(data.partialGraph);
    jsonstr += ",\"mode\":" + data.mode;
    jsonstr += ",\"systemName\":" +"\""+  systemName + "\"";
    if (data.mode == 0) {
        // This is active mode. must include another element called activeEdgeEnds;
        jsonstr += ",\"activeEdgeEnds\":{\"source\":" + data.activeEdgeEnds.source + ",\"object\":" + data.activeEdgeEnds.object + ",\"graphSource\":" + data.activeEdgeEnds.graphSource + ",\"graphObject\":" + data.activeEdgeEnds.graphObject + "}";
    }
    jsonstr += ",\"rejectedGraph\":" + getGraphString(data.rejectedGraph);
    jsonstr += ",\"dataGraphInUse\":" + data.dataGraphInUse;
    jsonstr += ",\"topk\":" + data.topk;
    jsonstr += ",\"refreshGraphNode\":" + data.refreshGraphNode;
    jsonstr += ",\"noOfIteration\":" + no_Of_Iterations;
    jsonstr += ",\"sessionId\":" +"\""+ sessionId.toString() + "\"";
    jsonstr += "}";
    return jsonstr;
}

jQuery("#query-keyword").on("input", function(){
    var value = document.getElementById("query-keyword").value;
    queryKeyword = value;
    updateQueryResultTable(false);
});

function updateQueryResultTable(isGraphUpdated) {
    var data;
    //if(queryKeyword != "") queryWindowNum = 1;
    jQuery.ajax({
        type: "GET",
        beforeSend: function(request) {
            request.setRequestHeader("Content-type", "application/json");
        },
        url: "https://xxx.uta.edu/orion_app/getqueryresult?querywindownum="+queryWindowNum+"&querywindowsize="+queryWindowSize+"&querykeyword="+queryKeyword+"&sessionid="+sessionId,
        processData: false,
        dataType: "json",
        async: true,
        success: function(data) {
            if(data[1]==undefined && queryWindowNum == 1 && queryKeyword === "") { //empty result
                jQuery("#query-modal-body").hide();
                jQuery("#no-query-modal-body").hide();
                jQuery("#no-result-modal-body").show();
            }
            else if(data[1]==undefined || data[0].length == data[1].length) { 
                jQuery("#no-query-modal-body").hide();
                jQuery("#no-result-modal-body").hide();
                jQuery("#query-modal-body").show();
                jQuery("#query-result-table tr").remove(); 
                var qt = document.getElementById("query-result-table");
                if(isGraphUpdated) {
                    queryWindowNum = 1;
                    document.getElementById("query-keyword").value = "";
                }
                if(queryWindowNum > 1) jQuery( "#result-back" ).show();
                else jQuery( "#result-back" ).hide();
                if(data.length > queryWindowSize+1) //only show next button if there's data on the next page
                    jQuery( "#result-next" ).show(); 
                else jQuery( "#result-next" ).hide();

                var typeCol = new Set()

                for(var i = 0; i < data.length; i++) {
                    if(i > queryWindowSize) break; //break when reached to the data on the next page
                    var cellNo = 0;
                    var row = qt.insertRow(i);
                    for(var j = 0; j < data[i].length; j++) {
                      if(i==0) {
                          for(var node of nodes) {
                              if(node.id==parseInt(data[i][j])) {
                                  var str = node.name.replace(/_/g, " ");
                                  if(str.toUpperCase()==str) { //show only if a it is a type column
                                    var cell = row.insertCell(cellNo);
                                    cell.innerHTML = "<b>"+str+"</b>";
                                    typeCol.add(j);
                                    cellNo++;
                                  }
                                  break;
                              }
                          }
                      }
                      else if(typeCol.has(j)) {
                        var cell = row.insertCell(cellNo);
                        cell.innerHTML = data[i][j];
                        cellNo++;
                      }
                    }
                }
            } else { //disconnected graph
                jQuery("#query-modal-body").hide();
                jQuery("#no-result-modal-body").hide();
                jQuery("#no-query-modal-body").show();
            }

        },
        // error: function() {
        //     nodeEditList.length = 0;
        //     data = ["123|Select Name", "234|Domain1", "4345|Domain2", "141|domain3", "3455|Domain4", "2134|Domain5", "7575|Domain6", "7575s|Domain7", "73275|Domain8", "75115|Domain9"];
        //     for (var i = 0; i < data.length; i++) {
        //         var temp = data[i].split("|");
        //         nodeEditList[i] = temp;
        //     }
        //     processGetRequest();
        // }
    });
}


function getRequestWithLoader(URL) {
    jQuery('#loading').modal('show');
    jQuery('#loading-indicator').show();
    var returnVar = [];
    var data;
    jQuery.ajax({
        type: "GET",
        beforeSend: function(request) {
            request.setRequestHeader("Content-type", "application/json");
        },
        url: URL,
        processData: false,
        dataType: "json",
        async: true,
        success: function(data) {
            for (var i = 0; i < data.length; i++) {
                var temp = data[i].split(",");
                nodeEditList[i] = temp;
            };
            processGetRequest();
        },
        error: function() {
            nodeEditList.length = 0;
            data = ["123|Select Name", "234|Domain1", "4345|Domain2", "141|domain3", "3455|Domain4", "2134|Domain5", "7575|Domain6", "7575s|Domain7", "73275|Domain8", "75115|Domain9"];
            for (var i = 0; i < data.length; i++) {
                var temp = data[i].split("|");
                nodeEditList[i] = temp;
            }
            processGetRequest();
        }
    });
}

function findEdgeEndTypes(node) {
  var typeList = [];
  for (var i = 0; i < links.length; i++) {
      if (links[i].linkID != -1 && links[i].source != links[i].target && links[i].source.greyflag == 0 && links[i].target.greyflag == 0 && links[i].flag == 2) {
          var endTypes = getEndTypesForEdge(links[i].linkID);
          if (links[i].source == node) {
            var domainType = endTypes[0][0]+":"+getDomainForType(parseInt(endTypes[0][0]))[1]+":"+endTypes[0][1].split('_').join(' ');
            if(typeList.includes(domainType) == false) {
              typeList.push(domainType);
            }
          } else if (links[i].target == node) {
            var domainType = endTypes[1][0]+":"+getDomainForType(parseInt(endTypes[1][0]))[1]+":"+endTypes[1][1].split('_').join(' ');
            if(typeList.includes(domainType) == false) {
              typeList.push(domainType);
            }
          }
      }
  }
  return typeList;
}

function loadEditTypeList(domainVal = -1, keyword = "") {
    var candEdges = "";
    var typesOptionList = "";
    curEndTypeList = [];
    for (var i = 0; i < links.length; i++) {
        if (links[i].source != links[i].target && links[i].source.greyflag == 0 && links[i].target.greyflag == 0) {
            var endTypes = getEndTypesForEdge(links[i].linkID);
            if (links[i].source == nodeToBeEdited) {
              var domainType = endTypes[0][0]+":"+getDomainForType(parseInt(endTypes[0][0]))[1]+":"+endTypes[0][1].split('_').join(' ');
              if(curEndTypeList.includes(domainType) == false) {
                curEndTypeList.push(domainType);
              }
              if (candEdges.length > 0) {
                  candEdges += "|" + links[i].linkID + ",0";
              } else {
                  candEdges += links[i].linkID + ",0";
              }
            } else if (links[i].target == nodeToBeEdited) {
              var domainType = endTypes[1][0]+":"+getDomainForType(parseInt(endTypes[1][0]))[1]+":"+endTypes[1][1].split('_').join(' ');
              if(curEndTypeList.includes(domainType) == false) {
                curEndTypeList.push(domainType);
              }
              if (candEdges.length > 0) {
                  candEdges += "|" + links[i].linkID + ",1";
              } else {
                  candEdges += links[i].linkID + ",1";
              }
            }
        }
    }

    jQuery("#node-edit-type-options").empty();
    if (!domainVal) {
        domainVal = -1;
    }

    if (!keyword) {
        keyword = "";
    }

    var url = "https://" + urlFull + "/orion_app/getnodetypecandidate?edges=" + candEdges + "&domain=" + domainVal + "&keyword=" + keyword;
    if (candEdges.length == 0) {
        // if(nodeToBeEdited.entity == -1 && nodeEditTypeWindowNo==0){
        //   setTypePageNumber(nodeToBeEdited.name, nodeToBeEdited.nodeID);
        // }

        //     url = "https://" + urlFull + "/orion_app/geteditnode?node="+nodeToBeEdited.nodeID+"&windownum="+0+"&windowsize="+windowSize+"&edges="+nodeEditEdges+"&keyword="+editNodeKeyword+"&domain="+domainVal;
        url = "https://" + urlFull + "/orion_app/gettypes?domain=" + domainVal + "&windownum=" + nodeEditTypeWindowNo + "&windowsize=100&keyword=" + keyword;
    }



    jQuery.ajax({
        type: "GET",
        beforeSend: function(request) {
            request.setRequestHeader("Content-type", "application/json");
        },
        url: url,
        processData: false,
        dataType: "json",
        async: false,
        success: function(data) {
            var typeList = [];
            for (var i = 0; i < data.length; i++) {
                var values = data[i].split(",");
                typeList.push(values);
            };
            for (var i = 0; i < typeList.length; i++) {
                //jQuery("#node-edit-type-options").append('<option value="' + typeList[i][0] + '" id="add-value-1" data-toggle="tooltip" data-container="#tooltip_container">' + typeList[i][1].toUpperCase() + '</option>');
                jQuery("#node-edit-type-options").append('<option value="' + typeList[i][0] + '" id="add-value-1">' + typeList[i][1].toUpperCase() + '</option>');
                typesOptionList += typeList[i][0] + ",";
            }
            if (data.length == 0) {
                jQuery("#node-edit-type-options").append('<option value="0" id="add-value-1" >Select Type...</option>');
                //jQuery("#node-edit-type-options").append('<option value="' + nodeToBeEdited.nodeID + '" id="add-value-1" data-toggle="tooltip" data-container="#tooltip_container">' + nodeToBeEdited.name + '</option>');
                jQuery("#node-edit-type-options").append('<option value="' + nodeToBeEdited.nodeID + '" id="add-value-1" >' + nodeToBeEdited.name + '</option>');
                typesOptionList += nodeToBeEdited.nodeID + ",";
            } else if (data.length < nodeEditKeywordWindowSize) {
                jQuery("#node-edit-type-next").attr("disabled", true);
            }
            // jQuery('#tooltip_container').remove();
            // jQuery('<div id="tooltip_container"></div>').insertAfter("#node-edit-type-options");
            jQuery("#node-edit-type").show();


        },
        error: function() {

        }
    });


    return typesOptionList.slice(0, -1);
}




function loadEditDomainList(typelist) {
    jQuery("#node-edit-domain-options").empty();
    var candEdges = "";
    for (var i = 0; i < links.length; i++) {
        if (links[i].source != links[i].target) {
            if (links[i].source == nodeToBeEdited) {
                if (candEdges.length > 0) {
                    candEdges += "|" + links[i].linkID + ",0";
                } else {
                    candEdges += links[i].linkID + ",0";
                }

            } else if (links[i].target == nodeToBeEdited) {
                if (candEdges.length > 0) {
                    candEdges += "|" + links[i].linkID + ",1";
                } else {
                    candEdges += links[i].linkID + ",1";
                }
            }
        }
    }

    var url = "https://" + urlFull + "/orion_app/getdomainsfortypes?types=" + typelist;
    if (candEdges.length == 0) {
        //     url = "https://" + urlFull + "/orion_app/geteditnode?node="+nodeToBeEdited.nodeID+"&windownum="+0+"&windowsize="+windowSize+"&edges="+nodeEditEdges+"&keyword="+editNodeKeyword+"&domain="+domainVal;
        url = "https://" + urlFull + "/orion_app/greeting";
    }

    jQuery.ajax({
        type: "GET",
        beforeSend: function(request) {
            request.setRequestHeader("Content-type", "application/json");
        },
        url: url,
        processData: false,
        dataType: "json",
        async: true,
        success: function(data) {
            var domainList = [];
            for (var i = 0; i < data.length; i++) {
                var values = data[i].split(",");
                domainList.push(values);
            };

            //                 jQuery("#node-edit-domain-options").append('<option value="-1" id="add-value-1" >Select Domain...</option>');
            for (var i = 0; i < domainList.length; i++) {
                jQuery("#node-edit-domain-options").append('<option class="domainlist" value="' + domainList[i][0] + '" id="add-value-1" >' + domainList[i][1].toUpperCase() + '</option>');

            }

            jQuery("#node-edit-domain").show();


        },
        error: function() {

        }
    });
}



function loadEditEntityList(domainVal = -1, typeVal = -1) {

    jQuery("#node-edit-entity-options").empty();
    var candEdges = "";
    for (var i = 0; i < links.length; i++) {
        if (links[i].source != links[i].target) {
            if (links[i].source == nodeToBeEdited) {
                if (candEdges.length > 0) {
                    candEdges += "|" + links[i].linkID + ",0";
                } else {
                    candEdges += links[i].linkID + ",0";
                }

            } else if (links[i].target == nodeToBeEdited) {
                if (candEdges.length > 0) {
                    candEdges += "|" + links[i].linkID + ",1";
                } else {
                    candEdges += links[i].linkID + ",1";
                }
            }
        }
    }

    var url = "https://" + urlFull + "/orion_app/geteditentity?typeid=" + nodeToBeEdited.type + "&entityid=" + nodeToBeEdited.nodeID + "&entityname=" + nodeToBeEdited.tempName + "&windownum=" + nodeEditWindowNo + "&windowsize=" + windowSize + "&edges=" + nodeEditEdges + "&keyword=" + editNodeKeyword;
    if (candEdges.length == 0 || domainVal != -1 || typeVal != -1) {
        if (nodeEditWindowNo == -1) {
            setEntityPageNumber(nodeToBeEdited.name, nodeToBeEdited.nodeID);
        }
        url = "https://" + urlFull + "/orion_app/getentities?domain=" + domainVal + "&windownum=" + nodeEditWindowNo + "&windowsize=100&type=" + typeVal + "&keyword=" + editNodeKeyword;
    }

    jQuery.ajax({
        type: "GET",
        beforeSend: function(request) {
            request.setRequestHeader("Content-type", "application/json");
        },
        url: url,
        processData: false,
        dataType: "json",
        async: true,
        success: function(data) {
            var entityList = [];
            for (var i = 0; i < data.length; i++) {
                var values = data[i].split(",");
                entityList.push(values);
            };
            if (entityList[entityList.length - 1][0] == "windowNum") {
                nodeEditWindowNo = parseInt(entityList[entityList.length - 1][1]);
                entityList.pop();
            }
            for (var i = 0; i < entityList.length; i++) {
                if (entityList[i][entityList[i].length - 1] == "preview") {
                    jQuery("#node-edit-entity-options").append('<option value="' + entityList[i][0] + '" id="add-value-1" data-html="true" data-toggle="tooltip" data-container="#tooltip_container">' + entityList[i].slice(1, entityList[i].length - 1).join() + '</option>'); //Changed by Heet
                } else {

                    jQuery("#node-edit-entity-options").append('<option value="' + entityList[i][0] + '" id="add-value-1" >' + entityList[i].slice(1, entityList[i].length - 1).join() + '</option>'); //Changed by Heet
                }
                //                     jQuery("#node-edit-entity-options").append('<option value="'+entityList[i][0]+'" id="add-value-1" >'+entityList[i][1]+'</option>');

            }
            jQuery('#tooltip_container').remove();
            jQuery('<div id="tooltip_container"></div>').insertAfter("#node-edit-entity-options");

            jQuery("#node-edit-entity").show();


        },
        error: function() {

        }
    });
}


function setEntityPageNumber(name, id) {

    var url = "https://" + urlFull + "/orion_app/getentityposition?name=" + name + "&id=" + id + "&windowsize=" + windowSize;

    jQuery.ajax({
        type: "GET",
        beforeSend: function(request) {
            request.setRequestHeader("Content-type", "application/json");
        },
        url: url,
        processData: false,
        dataType: "json",
        async: false,
        success: function(data) {
            nodeEditWindowNo = parseInt(data);

        },
        error: function() {

        }
    });
}



function setTypePageNumber(name, id) {

    var url = "https://" + urlFull + "/orion_app/gettypeposition?name=" + name + "&id=" + id + "&windowsize=" + windowSize;
    jQuery.ajax({
        type: "GET",
        beforeSend: function(request) {
            request.setRequestHeader("Content-type", "application/json");
        },
        url: url,
        processData: false,
        dataType: "json",
        async: false,
        success: function(data) {
            nodeEditTypeWindowNo = parseInt(data);

        },
        error: function() {

        }
    });
}



function processGetRequest() {

    jQuery('#loading-indicator').hide();
    jQuery('#loading').modal('hide');
    jQuery("#node-edit-type-options").empty();
    jQuery("#node-edit-entity-options").empty();
    jQuery("#node-edit-entity-options").attr('size', 19);

    jQuery("#node-edit-save-changes").css("background-color", "#828b8f");

    jQuery("#nodeEditModal").modal('show');
    jQuery("#edit-add-type-text").hide();
    jQuery("#edit-add-type-button").hide();
    jQuery("#node-edit-border").show();

    if (nodeToBeEdited.entity == -1) {
        //         for (var i = 0; i < nodeEditList.length; i++) {
        //             jQuery("#node-edit-entity-options").append('<option value="'+nodeEditList[i][0]+'" id="add-value-1" >'+nodeEditList[i][1]+'</option>');
        //         };
        jQuery('#node-edit-modal-title').text("Set a type/entity");
        for (var i = 0; i < nodeEditList.length; i++) {
            if (nodeEditList[i][nodeEditList[i].length - 1] == "preview") {
                jQuery("#node-edit-entity-options").append('<option value="' + nodeEditList[i][0] + '" id="add-value-1" data-html="true" data-toggle="tooltip" data-container="#tooltip_container">' + nodeEditList[i].slice(1, nodeEditList[i].length - 1).join() + '</option>'); //Changed by Heet
            } else {

                jQuery("#node-edit-entity-options").append('<option value="' + nodeEditList[i][0] + '" id="add-value-1" >' + nodeEditList[i].slice(1, nodeEditList[i].length - 1).join() + '</option>'); //Changed by Heet
            }
        };
        jQuery('#tooltip_container').remove();
        jQuery('<div id="tooltip_container"></div>').insertAfter("#node-edit-entity-options");
        //    jQuery('<div id="tooltip_container"></div>').attr("width",300px);
        jQuery("#node-edit-entity").show();
        jQuery("#node-edit-entity-search").show();
        jQuery("#node-edit-entity-back").attr("disabled", true);
        jQuery("#node-edit-entity-next").attr("disabled", false);
        if (nodeEditList.length <= windowSize) {
            jQuery("#node-edit-entity-next").attr("disabled", true);
        }
        //         jQuery("#node-edit-type").hide();
    } else {
        jQuery('#node-edit-modal-title').text("Set a type/entity")

        for (var i = 0; i < nodeEditList.length; i++) {
            jQuery("#node-edit-type-options").append('<option value="' + nodeEditList[i][0] + '" id="add-value-1">' + nodeEditList[i][1].toUpperCase() + '</option>');
        };
        jQuery('#tooltip_container').remove();
        jQuery('<div id="tooltip_container"></div>').insertAfter("#node-edit-type-options");
        jQuery("#node-edit-type").show();
        jQuery("#node-edit-entity").hide();
        jQuery("#node-edit-entity-search").hide();
    }
}




function postRequest(data, mode) {
    //   removeTempNodes();
    //         if(mode==4){
    //           if(mousedown_node) {
    //       // hide drag line
    //          drag_line
    //             .classed('hidden', true)
    //             .style('marker-end', '');
    //       }
    //       processSuccess(mode);
    //           return;
    //         }
    //         if (mode != 4){
    jQuery('#loading').modal('show');
    jQuery('#loading-indicator').show();
    //         }
    waitForJsonResultFlag = 1;
    no_Of_Iterations++;
    var jsonstr = myStringify(data);
    jQuery.ajax({
        type: "POST",
        //timeout: 4000, // sets timeout to 4 seconds
        beforeSend: function(request) {
            request.setRequestHeader("Content-type", "application/json");
            // request.setRequestHeader("Access-Control-Request-Method","POST");
            // request.setRequestHeader("Access-Control-Request-Headers","access-control-allow-origin, accept, content-type");
        },
        url: "https://" + urlFull + "/orion_app/greeting",
        data: jsonstr,
        //data: JSON.stringify(data),
        async: true,
        processData: false,
        // dataType: "json",
        error: function(xhr, textStatus, error) {
            // if (textStatus == 'timeout') {
            //     jQuery('#loading-indicator').hide();
            //     jQuery('#loading').modal('hide');
            //     removeSubSuggestionsTempNodes();
            //     // //try again
            //     // jQuery.ajax(this);         
            //     return;
            // }
            var err = eval("(" + xhr.responseText + ")");
            processError();
            // alert("No candidates");
            jQuery("#myModal3").modal("show");
        },
        success: function(msg) {
            linkTypes = [];
            linkTypes[0] = "0| Select Edge".split("|");
            for (var i = 0; i < msg.rankedUniqueEdges.length; i++) {
                linkTypes[i + 1] = msg.rankedUniqueEdges[i].edge.split("|");
            };
            returnObject = msg.rankedUniqueEdges;
            allReturnObject = msg.rankedEdges;
            processSuccess(mode);
            jQuery("#query-result-modal").show();
            updateQueryResultTable(true);
        }
    });
    if (mode == 4) {
        if (mousedown_node) {
            // hide drag line
            drag_line
                .classed('hidden', true)
                .style('marker-end', '');
        }
    }
}

function compareStr(a, b) {
    if (a[1] === b[1]) {
        return 0;
    } else {
        return (a[1] < b[1]) ? -1 : 1;
    }
}

function processSuccess(mode) {
    var errorFlag = 0;
    if (mode == 1 || mode == 2) {
        var i = 0;
        for (i = 0;
            (i < noOfSuggestions) && (i < returnObject.length); i++) {
            var entry = returnObject[i];
            for (var j = 0; j < nodes.length; j++) {

                if (nodes[j].nodeID == entry.source.split('|')[0] && nodes[j].id == entry.graphSource) {
                    var entityValue;
                    if (entry.isObjectType == true) {
                        entityValue = -1;
                    } else {
                        entityValue = entry.object.split('|')[0];
                    }

                    var newNode = null;
                    for (var k = 0; k < nodes.length; k++) {
                        if (nodes[k].nodeID == entry.object.split('|')[0] && nodes[k].id == entry.graphObject) {
                            //                       makeNew = false;
                            newNode = nodes[k];
                            break;
                        }
                    }
                    if (newNode == null) {
                        var newNode = {
                            id: ++lastNodeId,
                            name: entry.object.split('|')[1],
                            type: entry.source.split('|')[0],
                            flag: 2,
                            greyflag: 1,
                            nodeID: entry.object.split('|')[0],
                            entity: entityValue,
                            tempName: entry.object.split('|')[1]
                        };
                        nodes.push(newNode);
                        var link = {
                            source: nodes[j],
                            target: newNode,
                            value: entry.edge.split('|')[1],
                            id: ++lastEdgeID,
                            flag: 2,
                            linkID: entry.edge.split('|')[0],
                            linkNum: 1,
                            actualSourceType: entry.actualSourceType,
                            actualTargetType: entry.actualObjectType
                        };
                    } else {
                        var link = {
                            source: nodes[j],
                            target: newNode,
                            value: entry.edge.split('|')[1],
                            id: ++lastEdgeID,
                            flag: 20,
                            linkID: entry.edge.split('|')[0],
                            linkNum: 1,
                            actualSourceType: entry.actualSourceType,
                            actualTargetType: entry.actualObjectType
                        };
                    }

                    //                   var link = {source: nodes[j], target: newNode, value: entry.edge.split('|')[1], id: ++lastEdgeID, flag: 2 , linkID: entry.edge.split('|')[0], linkNum: 1, actualSourceType: entry.actualSourceType, actualTargetType: entry.actualObjectType};
                    links.push(link);
                    break;
                } else if (nodes[j].nodeID == entry.object.split('|')[0] && nodes[j].id == entry.graphObject) {
                    var entityValue;
                    if (entry.isSourceType == true) {
                        entityValue = -1;
                    } else {
                        entityValue = entry.source.split('|')[0];
                    }
                    var newNode = null;
                    for (var k = 0; k < nodes.length; k++) {
                        if (nodes[k].nodeID == entry.source.split('|')[0] && nodes[k].id == entry.graphSource) {
                            //                       makeNew = false;
                            newNode = nodes[k];
                            break;
                        }
                    }
                    if (newNode == null) {
                        var newNode = {
                            id: ++lastNodeId,
                            name: entry.source.split('|')[1],
                            flag: 2,
                            greyflag: 1,
                            type: entry.source.split('|')[0],
                            nodeID: entry.source.split('|')[0],
                            entity: entityValue,
                            tempName: entry.source.split('|')[1]
                        };
                        nodes.push(newNode);
                        var link = {
                            source: newNode,
                            target: nodes[j],
                            value: entry.edge.split('|')[1],
                            id: ++lastEdgeID,
                            flag: 2,
                            linkID: entry.edge.split('|')[0],
                            linkNum: 1,
                            actualSourceType: entry.actualSourceType,
                            actualTargetType: entry.actualObjectType
                        };
                    } else {
                        //                   var newNode = {id: ++lastNodeId, reflexive: false, name: entry.source.split('|')[1], flag : 2, greyflag: 1, nodeID: entry.source.split('|')[0], entity: entityValue, tempName: entry.source.split('|')[1]};
                        //                   nodes.push(newNode);
                        var link = {
                            source: newNode,
                            target: nodes[j],
                            value: entry.edge.split('|')[1],
                            id: ++lastEdgeID,
                            flag: 20,
                            linkID: entry.edge.split('|')[0],
                            linkNum: 1,
                            actualSourceType: entry.actualSourceType,
                            actualTargetType: entry.actualObjectType
                        };
                    }
                    links.push(link);
                    break;
                    // links.push({source: node, target: newNode, value: entry.edge.split('1')[1], id: ++lastEdgeID, flag: 2,  linkID: entry.edge.split('1')[0]});
                }
            };
        };
        suggestionCounter += i;
        suggestions = true;
        suggestionPicked = false;
        noOfSuggestionsPicked = 0;
        // suggestionCounter = i+j;

        restart();
    } else if (mode == 3) {
        if (linkTypes.length != 1) {
            jQuery("#edge-options").empty();
            for (var i = 0; i < linkTypes.length; i++) {
                jQuery("#edge-options").append('<option value="' + linkTypes[i][0] + '" id="add-value-1" >' + linkTypes[i][1] + '</option>');
            };
        } else {
            errorFlag = 1;
        }

        restart();
    } else if (mode == 4) {
        newEdge = true;
        restart();
        if (selected_link != null && selected_link.flag != 2 && selected_link.flag != 20) {
            jQuery("#linkId_" + d.id).css('stroke', '#2EFEF7');
        } else {
            //Orange removed// jQuery("#linkId_"+selected_link.id).css('stroke', 'orange');
        }
        if (linkTypes.length != 1) {
            //              jQuery("#myModal").modal('show');
            //              jQuery("#type-div").hide();
            //              jQuery("#entity-div").hide();
            //              jQuery("#selected-div").hide();
            //              jQuery("#domain-div").hide();
            //              jQuery("#keyword-div").hide();
            //              jQuery("#type-keyword-div").hide();
            //              jQuery("#type-border").hide();
            //              jQuery("#entity-border").hide()
            //              jQuery("#edge-options").empty();
            //              jQuery("#modal-title").empty();
            //              jQuery("#modal-title").append("<h4>New Edge Label</h4>");
            for (var i = 0; i < linkTypes.length; i++) {
                jQuery("#edge-options").append('<option value="' + linkTypes[i][0] + '" id="add-value-1" >' + linkTypes[i][1] + '</option>');
            };
            /* Edited by AD */
            var unconnected = null;
            // for (var y = 0; y < nodes.length; y++) {
            //     var unc_flag = false;
            //     for (var z = 0; z < links.length; z++) {
            //         if (links[z].source != links[z].target && (links[z].source.id == nodes[y].id || links[z].target.id == nodes[y].id)) {
            //             unc_flag = true;
            //         }
            //
            //     }
            //     if (!unc_flag) {
            //         unconnected = nodes[y].id;
            //         break;
            //     }
            // }



            if (unconnected != null) {
                if (unconnected == selected_link.source.id || unconnected == selected_link.target.id) {
                    jQuery("#myModal").modal('show');
                    jQuery("#type-div").hide();
                    jQuery("#entity-div").hide();
                    jQuery("#selected-div").hide();
                    jQuery("#domain-div").hide();
                    jQuery("#keyword-div").hide();
                    jQuery("#type-keyword-div").hide();
                    jQuery("#type-border").hide();
                    jQuery("#entity-border").hide();
                    jQuery("#add-type-text").hide();
                    jQuery("#add-type-button").hide();
                    jQuery("#type-list").hide();
                    jQuery("#edge-options").empty();
                    jQuery("#modal-title").empty();
                    jQuery("#modal-title").append("<h4>New Edge Label</h4>");
                    jQuery("#dtl").hide();

                    for (var i = 0; i < linkTypes.length; i++) {
                        jQuery("#edge-options").append('<option value="' + linkTypes[i][0] + '" id="add-value-1" >' + linkTypes[i][1] + '</option>');
                    }
                    jQuery("#edge-div").show();
                } else {
                    selected_link.source = selected_link.target;
                    selected_link = null;
                    //     allowNodeDrag = true;
                    //     translateAllowed = true;
                    //     drawEdge = false;
                    allowNodeCreation = false;
                    checkKeywordStatus();
                    restart();
                }
            } else {
                jQuery("#myModal").modal('show');
                jQuery("#type-div").hide();
                jQuery("#entity-div").hide();
                jQuery("#selected-div").hide();
                jQuery("#domain-div").hide();
                jQuery("#keyword-div").hide();
                jQuery("#type-keyword-div").hide();
                jQuery("#type-border").hide();
                jQuery("#entity-border").hide();
                jQuery("#add-type-text").hide();
                jQuery("#add-type-button").hide();
                jQuery("#type-list").hide();
                jQuery("#edge-options").empty();
                jQuery("#modal-title").empty();
                jQuery("#modal-title").append("<h4>New Edge Label</h4>");
                jQuery("#dtl").hide();

                for (var i = 0; i < linkTypes.length; i++) {
                    jQuery("#edge-options").append('<option value="' + linkTypes[i][0] + '" id="add-value-1" >' + linkTypes[i][1] + '</option>');
                }
                jQuery("#edge-div").show();
            }

            /* Edited by AD */
        } else {
            errorFlag = 1;
            selected_link.source = selected_link.target;
        }
    }
    //if (mode != 4){
    jQuery('#loading-indicator').hide();
    jQuery('#loading').modal('hide');
    //}
    if (mode != 4) {
        displayFlag = 1;
    }
    waitForJsonResultFlag = 0;
    mouseup();
    if (errorFlag == 1) {
        //       allowNodeDrag = true;
        //       translateAllowed = true;
        //       drawEdge = false;
        allowNodeCreation = false;
        //alert("The system was not able to find any suggestions for the edge.");
        jQuery("#myModal2").modal("show");
    }
}


function processError() {
    jQuery('#loading-indicator').hide();
    jQuery('#loading').modal('hide');
    waitForJsonResultFlag = 0;
    mouseup();
}



function keywordUpdate(valueKeyword) {

    selected_node.name = valueKeyword[1];
    selected_node.nodeID = valueKeyword[0];
    console.log(valueKeyword[1] + "\n" + selected_node.name);
    SetKeywordFlag();

    /*if (keywordCategory == 3)
    {
          var textEntity = jQuery("#entity-options option:selected").text();
          var valueEntity = jQuery("#entity-options option:selected").val();
          var valueType = jQuery("#type-options option:selected").val();
          var valueDomain = jQuery("#domain-options option:selected").val();
          if (textEntity!= "Select Type...")
          {
            selected_node.name = textEntity;
            selected_node.nodeID = valueEntity;
            selected_node.entity = valueEntity;
            selected_node.type = valueType;
            selected_node.domain = valueDomain;
            console.log(valueEntity + "\n" + selected_node.name);
         }
         else{
           jQuery("#modal-text").append("<p>Please Select valid Entity</p>");
        }
    }
    else if (keywordCategory == 2)
    {
          var textType = jQuery("#type-options option:selected").text();
          var valueType = jQuery("#type-options option:selected").val();
          var valueDomain = jQuery("#domain-options option:selected").val();
          if (textType!= "Select Type...")
          {
            selected_node.name = textType;
            selected_node.nodeID = valueType;
            selected_node.type = valueType;
            selected_node.domain = valueDomain;
            console.log(valueType + "\n" + selected_node.name);
         }
     }*/
    restart();
}

function updateDomainsTypesEntities(typeKeywordValue, entityKeywordValue) {
  var typeValue = -1;
  var domainValue = -1;
  if(jQuery("#domain-options option:selected").val() != undefined) {
    domainValue = jQuery("#domain-options option:selected").val();
  }
  if(jQuery("#type-options option:selected").val() != undefined) {
    typeValue = jQuery("#type-options option:selected").val();
  }

  var entityWindowNumber = entityCount - 1;
  var typeWindowNumber = typeCount - 1;

  if(curTypeList.concat(curEndTypeList).length != 0) {
    var nodeTypeValues = "";
    for(var elem of curTypeList.concat(curEndTypeList)) {
      if(nodeTypeValues == "") {
        nodeTypeValues = elem.split(':')[0];
      } else {
        nodeTypeValues = nodeTypeValues+ "," + elem.split(':')[0];
      }
    }

    var entityId = -1;
    if(nodeToBeEdited != null && nodeToBeEdited.entity != -1) {
      entityId = nodeToBeEdited.nodeID;
    }

    // var neighborEntities = "";
    // var edgesInfo = [];
    // var nodesInfo = [];
    // var seenNeighbors = [];
    //
    // for(var i = 0; i < links.length; i++) {
    //   var n = "";
    //   if(links[i].source == nodeToBeEdited && links[i].target.entity != -1) {
    //     n = links[i].target.entity+","+links[i].linkID.toString()+",true";
    //   } else if (links[i].target == nodeToBeEdited && links[i].source.entity != -1) {
    //     n = (links[i].source.entity+","+links[i].linkID.toString()+",false");
    //   }
    //   if(n != "" && !seenNeighbors.includes(n)) {
    //     if (neighborEntities == "") {
    //       neighborEntities += n;
    //     } else {
    //       neighborEntities += ("|"+n);
    //     }
    //   }
    //   if(links[i].source != links[i].target && links[i].source.greyflag == 0 && links[i].target.greyflag == 0) {
    //     edgesInfo.push(links[i].source.id.toString()+":"+links[i].source.entity.toString()+","+links[i].linkID+","+links[i].target.id.toString()+":"+links[i].target.entity.toString());
    //   }
    // }

    var edgeEndTypesMap = new Map();
    for(var i = 0; i < nodes.length; i++) {
      // n = nodes[i].id.toString();
      // //type values assigned by user
      // if(nodes[i].typeList != null) {
      //   for(var x of nodes[i].typeList) {
      //       n += (","+x.split(":")[0]);
      //   }
      // }
      //type constrain from incident edges
      var edgeEndTypes = findEdgeEndTypes(nodes[i]);
      if(edgeEndTypes.length > 0) {
        edgeEndTypesMap.set(nodes[i].id, edgeEndTypes);
      }
      // if(edgeEndTypes != null) {
      //   for(var x of edgeEndTypes) {
      //       n += (","+x.split(":")[0]);
      //   }
      // }
      // if (nodes[i].greyflag == 0 && !nodesInfo.includes(n)) {
      //   nodesInfo.push(n);
      // }
    }
    // console.log(nodesInfo);
    // console.log(edgesInfo);

    //createpartialgraph
    partialGraph.length = 0;
    for (var i = 0, j = 0; i < links.length; i++) {
        if (links[i].linkID != -1 && (links[i].source != links[i].target) && (links[i].source.greyflag == 0 && links[i].target.greyflag == 0) && links[i].flag == 2) { //dotted edge between existing nodes when flag 20
            var srcVals = (links[i].source == nodeToBeEdited) ? nodeTypeValues : getTypeValues(links[i].source.typeList)+","+getTypeValues(edgeEndTypesMap.get(links[i].source.id));
            var objVals = (links[i].target == nodeToBeEdited) ? nodeTypeValues : getTypeValues(links[i].target.typeList)+","+getTypeValues(edgeEndTypesMap.get(links[i].target.id));
            partialGraph[j] = {
                source: links[i].source.nodeID,
                graphSource: links[i].source.id,
                edge: links[i].linkID,
                object: links[i].target.nodeID,
                graphObject: links[i].target.id,
                sourceTypeValues: srcVals,
                objectTypeValues: objVals,
                sourceEntity: links[i].source.entity,
                objectEntity: links[i].target.entity
            };
            j++;
        }
    };
    for (var l = 0, k = partialGraph.length; l < nodes.length; l++) {
      if(nodes[l].greyflag != 0) continue;
        var checklink = false;
        for (var i = 0; i < links.length; i++) {
            if (links[i].linkID != -1 && links[i].flag != 20 && (nodes[l] == links[i].source || nodes[l] == links[i].target) && (links[i].source.greyflag == 0 && links[i].target.greyflag == 0) && links[i].source != links[i].target) {
                checklink = true;
            }
        }
        if (!checklink) {
            var srcVals = (nodes[l] == nodeToBeEdited || nodes[l] == selected_node) ? nodeTypeValues : getTypeValues(nodes[l].typeList)+","+getTypeValues(edgeEndTypesMap.get(nodes[l].id));
            partialGraph[k] = {
                source: nodes[l].nodeID,
                graphSource: nodes[l].id,
                edge: -1,
                object: 0,
                graphObject: -1,
                sourceTypeValues: srcVals,
                objectTypeValues: "",
                sourceEntity: nodes[l].entity,
                objectEntity: -1
            };
            k++;
        }
    }
    var data = {
        partialGraph: partialGraph
    };
    var jsonstr =  "{" + "\"partialGraph\":" + getGraphString(data.partialGraph) + "}";
    var url = "https://" + urlFull + "/orion_app/getdomainstypesentities?domain=" + domainValue + "&typewindownum=" + typeWindowNumber + "&entityewindownum=" + entityWindowNumber + "&windowsize=" + entityWindowSize + "&type=" + typeValue + "&typeKeyword=" + typeKeywordValue+ "&entityKeyword=" + entityKeywordValue + "&nodetypevalues=" + nodeTypeValues + "&entityid=" + entityId+ "&sessionid=" + sessionId;
    if(nodeToBeEdited != undefined) url = url + "&nodetobeedited=" + nodeToBeEdited.id;
    jQuery.ajax({
        type: "POST",
        beforeSend: function(request) {
            request.setRequestHeader("Content-type", "application/json");
        },
        url: url,
        processData: false,
        dataType: "json",
        data: jsonstr,
        async: false,
        success: function(msg) {
          // setTimeout(function() {
          //   delaySuccess(msg);
          // }, 10000);

          if(typeKeywordValue == "" && entityKeywordValue == "") {
            generatedDomains.length = 0;
            for (var i = 0; i < msg[0].length; i++) {
                generatedDomains[i] = msg[0][i].split(",");
            }
          }

          if(entityKeywordValue == "") {
            generatedTypes.length = 0;
            for (var i = 0; i < msg[1].length; i++) {
                generatedTypes[i] = msg[1][i].split(",");
            }
          }

          if(typeKeywordValue == "") {
            generatedEntities.length = 0;
            for (var i = 0; i < msg[2].length; i++) {
                generatedEntities[i] = msg[2][i].split(",");
            }
          }
        }
    });
  } else {
    generatedDomains = allDomains.slice();
    generatedTypes = allTypes.slice();
    generatedEntities = allEntities.slice();
  }
}

function setToFirstPage() {
  //set to first page
  typeCount = 1;
  entityCount = 1;
  jQuery("#type-next").attr("disabled", false);
  jQuery("#type-back").attr("disabled", true);
  jQuery("#entity-next").attr("disabled", false);
  jQuery("#entity-back").attr("disabled", true);
}

// app starts here
svg.on('mousedown', mousedown)
    .on('mousemove', function() {
        if (!mousedown_node) return;
        // update drag line

        //drag_line.attr('d', 'M' + mousedown_node.x + ',' + mousedown_node.y + 'L' + d3.mouse(this)[0] + ',' + d3.mouse(this)[1]);
        drag_line.attr('d', 'M' + mousedown_node.x + ',' + mousedown_node.y + 'A' + 0 + ',' + 0 + ' 0 0,1 ' + d3.mouse(this)[0] + ',' + d3.mouse(this)[1]);
        restart();
    })
    .on('mouseup', mouseup)
    .on("contextmenu", function() {

    });
d3.select(window)
    .on('keydown', keydown)
    .on('keyup', keyup);


restart();




jQuery("#useful-tips-div").on('click', function() {
    if (usefulTipsFlag == 0) {
        CheckOtherWraps();
        usefulTipsFlag = 1;
    } else {
        usefulTipsFlag = 0;
    }
    jQuery("#wrap2").toggleClass('active');

    jQuery("#useful-tips").toggleClass('active');
    return false;
});



jQuery("#useful-tips").on('click', function() {
    if (usefulTipsFlag == 0) {
        CheckOtherWraps();
        usefulTipsFlag = 1;
    } else {
        usefulTipsFlag = 0;
    }
    jQuery("#wrap2").toggleClass('active');

    jQuery("#useful-tips").toggleClass('active');
    return false;
});




jQuery("#settings-div").on('click', function() {
    if (settingsFlag == 0) {
        CheckOtherWraps();
        settingsFlag = 1;
    } else {
        settingsFlag = 0;
    }
    jQuery("#wrap3").toggleClass('active');

    jQuery("#settings").toggleClass('active');
    return false;
});

jQuery("#possible-actions-button").on('click', function() {
    animatedHelp();
    jQuery("#myModalPossibleActions").modal('show');
});


jQuery("#settings").on('click', function() {
    if (settingsFlag == 0) {
        CheckOtherWraps();
        settingsFlag = 1;
    } else {
        settingsFlag = 0;
    }
    jQuery("#wrap3").toggleClass('active');

    jQuery("#settings").toggleClass('active');
    return false;
});




jQuery("#edge-types-div").on('click', function() {
    if (edgeTypesFlag == 0) {
        CheckOtherWraps();
        edgeTypesFlag = 1;
    } else {
        edgeTypesFlag = 0;
    }
    jQuery("#wrap4").toggleClass('active');

    jQuery("#edge-types").toggleClass('active');
    return false;
});


jQuery("#edge-types").on('click', function() {
    if (edgeTypesFlag == 0) {
        CheckOtherWraps();
        edgeTypesFlag = 1;
    } else {
        edgeTypesFlag = 0;
    }
    jQuery("#wrap4").toggleClass('active');

    jQuery("#edge-types").toggleClass('active');
    return false;
});


// jQuery('#edit-dtl').on('click', '.editbtn', function(){
//    var row = jQuery(this).closest('tr');
//    var domain = row.find('td')[0].innerText;
//    var type = row.find('td')[1].innerText;
//    for(var i = 0; i < curTypeList.length; i++) {
//      if(curTypeList[i].endsWith(domain+":"+type)) {
//        curTypeList.splice(i,1);
//        break;
//      }
//    }
//    //deletedTypes.push(domain+":"+type);
//    row.remove();
// });

jQuery('#dtl').on('click', '.editbtn', function(){
   var row = jQuery(this).closest('tr');
   var domain = row.find('td')[0].innerText.toLowerCase();
   var type = row.find('td')[1].innerText.toLowerCase();
   for(var i = 0; i < curTypeList.length; i++) {
     if(curTypeList[i].endsWith(domain+":"+type)) {
       curTypeList.splice(i,1);
       break;
     }
   }
   // var text = jQuery("#type-options option:selected").text();
   // var value = jQuery("#type-options option:selected").val();
   // if(text == type && getDomainForType(value)[1]==domain) {
   //   jQuery("#add-type-text").html("Require this node to be in Type <b>"+type.toUpperCase()+"</b> (in Domain <i><b>"+domain.toUpperCase()+"</b></i>)?").show();
   //   jQuery("#add-type-button").show();
   // }
   //deletedTypes.push(domain+":"+type);
   row.remove();
   toggleConfirmButton("");
   setToFirstPage();

   updateDomainsTypesEntities("", "");
   domains = generatedDomains.slice();
   types = generatedTypes.slice();
   entities = generatedEntities.slice();
   displayDomainOptions(domains);
   displayTypeOptions(types);
   displayEntityOptions(entities);
});

// jQuery('#edit-dtl').on('click', '.editbtn', function(){
//    var row = jQuery(this).closest('tr');
//    var domain = row.find('td')[0].innerText.toLowerCase();
//    var type = row.find('td')[1].innerText.toLowerCase();
//    for(var i = 0; i < curTypeList.length; i++) {
//      if(curTypeList[i].endsWith(domain+":"+type)) {
//        curTypeList.splice(i,1);
//        break;
//      }
//    }
//    var text = jQuery("#node-edit-type-options option:selected").text().toLowerCase();
//    var value = jQuery("#node-edit-type-options option:selected").val();
//    if(text == type && getDomainForType(value)[1]==domain) {
//      jQuery("#edit-add-type-text").html("Require this node to be in Type <b>"+type.toUpperCase()+"</b> (in Domain <i><b>"+domain.toUpperCase()+"</b></i>)?").show();
//      jQuery("#edit-add-type-button").show();
//    }
//    //deletedTypes.push(domain+":"+type);
//    row.remove();
//    toggleConfirmButton("node-edit-");
// });

function fetchWiki(target) {
    let tooltipText = "d";

    jQuery.ajax({
        url: "https://xxx.uta.edu/orion_app/getwikisummary?nodeid=" + target.value,
        type: 'get',
        async: false,
        success: function(response) {
            jQuery(target).attr('data-original-title', response + "<b>bold text</b>");
        }
    });
    return tooltipText.replace(/_/g, " ");
}

function getDomainForType(target) {
    if(domainForTypeMap.has(target)) {
      return domainForTypeMap.get(target);
    }
    let domain = "";
    let domainid = "";
    jQuery.ajax({
        type: "GET",
        beforeSend: function(request) {
            request.setRequestHeader("Content-type", "application/json");
        },
        url: "https://xxx.uta.edu/orion_app/getdomainsfortypes?types=" + target,
        processData: false,
        dataType: "json",
        async: false,
        success: function(data) {
            domain = (data.length > 1) ? data[1].split(",")[1] : "";
            domainid = (data.length > 1) ? data[1].split(",")[0] : -1;
        }
    })
    if(domainid != -1) domainForTypeMap.set(target, [domainid, domain]);
    return [domainid, domain];
}

function getEndTypesForEdge(target) {
  if(endTypesForEdgeMap.has(target)) {
    return endTypesForEdgeMap.get(target);
  }
  let source = "";
  let object = "";
  jQuery.ajax({
      type: "GET",
      beforeSend: function(request) {
          request.setRequestHeader("Content-type", "application/json");
      },
      url: "https://xxx.uta.edu/orion_app/getendtypes?edge=" + target,
      processData: false,
      dataType: "text",
      async: false,
      success: function(data) {
          source = data.split('|')[0].split(',');
          object = data.split('|')[1].split(',');
      }
  })
  endTypesForEdgeMap.set(target, [source, object]);
  return [source, object];
}


// jQuery('body').tooltip({
// delay: 500,
//    placement: "bottom",
//    title: "default",
//    html: true,
// selector: "[data-toggle='tooltip']"});



// jQuery(document).on("show.bs.tooltip", "[data-toggle='tooltip']", function(el) {
// fetchWiki(el.target);
// });



/*jQuery("#submit-clear").on('click', function(){
  jQuery("#wrap5").toggleClass('active');

  jQuery("#submit-clear").toggleClass('active');
  return false;
});*/




jQuery("#wrap2-x-button").click(function() {
    if (usefulTipsFlag == 0) {
        usefulTipsFlag = 1;
    } else {
        usefulTipsFlag = 0;
    }
    jQuery("#wrap2").toggleClass('active');
    jQuery("#useful-tips").toggleClass('active');
});

jQuery('#x-button').click(function() {
    jQuery("#save-changes").css("background-color", "#828b8f");
    if(curNodeLabel != null && selected_node != null) {
      selected_node.name = curNodeLabel;
      selected_node.tempName = curNodeLabel;
    }

    restart();
});

jQuery("#wrap3-x-button").click(function() {
    if (settingsFlag == 0) {
        settingsFlag = 1;
    } else {
        settingsFlag = 0;
    }
    jQuery("#wrap3").toggleClass('active');
    jQuery("#settings").toggleClass('active');
});


jQuery("#wrap4-x-button").click(function() {
    if (edgeTypesFlag == 0) {
        edgeTypesFlag = 1;
    } else {
        edgeTypesFlag = 0;
    }
    jQuery("#wrap4").toggleClass('active');
    jQuery("#edge-types").toggleClass('active');
    restart();
});

function CheckOtherWraps() {
    if (usefulTipsFlag == 1) {
        jQuery("#wrap2").toggleClass('active');
        jQuery("#useful-tips").toggleClass('active');
        usefulTipsFlag = 0;
    }
    if (settingsFlag == 1) {
        jQuery("#wrap3").toggleClass('active');
        jQuery("#settings").toggleClass('active');
        settingsFlag = 0;
    }
    if (edgeTypesFlag == 1) {
        jQuery("#wrap4").toggleClass('active');
        jQuery("#edge-types").toggleClass('active');
        edgeTypesFlag = 0;
    }
    if (clearButtonFlag == 1) {
        jQuery("#wrap5").toggleClass('active');
        clearButtonFlag = 0;
    }
}

function clearAllSelection() {
  jQuery("#save-changes").css("background-color", "#828b8f");
  jQuery("#domain-options option:selected").attr('selected', false);
  jQuery("#type-options option:selected").attr('selected', false);
}

function collectDumpVariables() {
    document.getElementById("entry_425796398").value = getGraphStringUserStudy(partialGraph, true);
    document.getElementById("entry_2022483379").value = getGraphStringUserStudy(rejectedGraph, false);
    document.getElementById("entry_1515945795").value = no_Of_Iterations;
    document.getElementById("entry_982064888").value = sessionId;
    document.getElementById("entry_982064888_1").value = sessionId;
    document.getElementById("entry_5453654645").value = version;
    document.getElementById("entry_5453654646").value = systemName;
    document.getElementById("entry_5453654646_1").value = systemName;
    //   document.cookie =  "name=" + document.getElementById("entry_802961755").value + ";" ;
    console.log("test");
    console.log(document.getElementById("entry_1515945795").value);
}
