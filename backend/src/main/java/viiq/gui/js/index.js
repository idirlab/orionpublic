var entitiesList = [];
var typesList = [];
var entityWindowSize = 100;
var typeWindowSize = 100;
var keywordFlag = 0;
var entityCount = 0;
var typeCount = 0;
var entitiesThruTypesFlag = 0;
var entitiesThruDomainFlag = 0;
var typesThruDomainFlag = 0;
var searchLength = 3;
var entityKeywordSearchFlag = 0;
var typeKeywordSearchFlag = 0;
var entityKeywordSearch;
var typeKeywordSearch;
var t5; // TextboxList object created for entity
var t6; // TextboxList object created for type
var t7; // TextboxList object created for node edit entity
var getExamplesFlag = 0;
var nodeEditFlag = 0;
var mouseOverFlagOfIndex = false;
function clearIndexGlobalVariables(){
    keywordFlag = 0;
    entityCount = 0;
    typeCount = 0;
    entitiesThruTypesFlag = 0;
    entitiesThruDomainFlag = 0; 
    typesThruDomainFlag = 0;
    entityKeywordSearchFlag = 0;
    typeKeywordSearchFlag = 0;
    getExamplesFlag = 0;
}


function resetKeywordFlag()
{
    keywordFlag = 0;
}


function nodeNameText(selected_node, context){
  if(context == 1){
    jQuery("#modal-text").empty();
    jQuery('#modal-text').append("<h4>Instructions</h4>"+
                        "<ul>"+
                          "<li>Click on 'close' to delete the newly added node.</li>"+
                          "<li>Select a domain value from the <b>Domain</b> dropdown box, followed by a type from the Type dropdown box.</li>"+
                          "<li>Select an exact entity as the node label from the <b>Entity</b> dropdown box.</li>"+
                          "<li>You can select type and entity value for a node or just type or just entity value for a node. Domain value for a node is optional.</li>"+
                          "<li>You can also search for type and entity values using the search box provided for each.</li>"+
                          "<li>Click the Save button to apply the selected node label.</li>"+
                        "</ul>");
  }else if(context == 2){
    jQuery('#modal-text').append("<h4>Instructions</h4>"+
                        "<ul>"+
                          "<li>click 'close' to delete the created node.</li>"+
                          "<li>click 'save changes' to exit the window.</li>"+
                        "</ul>");
  }else if(context == 3){
    jQuery('#modal-text').append("<h4>Instructions</h4>"+
                        "<ul>"+
                          "<li>Edge labels are ranked by their relevance in the <b>Edge Label</b> dropdown box. Choose the appropriate label to use from the list.</li>"+
                          "<li>Click the Save button to apply the selected edge label.</li>"+
                        "</ul>");
  }
  //jQuery("#show-instructions").attr("disabled", true);
}

function SetKeywordFlag()
{
    keywordFlag = 1;
}




jQuery("#type-back").click(function(){
   event.stopPropagation()
   jQuery("#type-next").attr("disabled",false);
   var domainValue;
   var keywordValue;
   typeCount = typeCount - 1;
   var windowNo = typeCount - 1;
   typesList.length = 0;
   var typeValue = -1;
   if (typeKeywordSearchFlag == 1)
   {
           keywordValue = typeKeywordSearch;
           if (typesThruDomainFlag == 1){
                 domainValue = jQuery("#domain-options option:selected").val();
           }  
           else
           { //get all types  with keyword
               domainValue = -1;
           } 
   }
   else if (typesThruDomainFlag == 1){
                 domainValue = jQuery("#domain-options option:selected").val();
                 keywordValue="";
   }  
   else
   { //get all types
      domainValue = -1;
      keywordValue = "";
   }
   getRequest("http://[server_name]/orion_app/gettypes?domain="+domainValue+"&windownum="+windowNo+"&windowsize="+typeWindowSize+"&keyword="+keywordValue, typesList);
   if(typesList.length < 19){
      jQuery("#types-options").attr('size',typesList.length);}
   else{
   jQuery("#type-options").attr('size',19);
   }
   if (typesList.length <= typeWindowSize){
       jQuery("#type-next").attr("disabled", true);}
  jQuery("#type-options").empty();
  for (var i = 0; i < typesList.length; i++) {
    jQuery("#type-options").append('<option value="'+typesList[i][0]+'" id="type-value-1">'+typesList[i][1]+'</option>');
  };
   if(typeCount == 1){
      jQuery("#type-back").attr("disabled",true);}
});
   





jQuery("#entity-back").click(function(){
   event.stopPropagation()
   jQuery("#entity-next").attr("disabled",false);
   var domainValue;
   var typeValue;
   var keywordValue;
   entityCount = entityCount - 1;
   var windowNo = entityCount - 1;
   entitiesList.length = 0;
   if (entityKeywordSearchFlag == 1)
   {
 
           keywordValue = entityKeywordSearch;
           if (entitiesThruTypesFlag == 1){
                //typeValue = jQuery("#type-options option:selected").val();
                typeValue = jQuery("#type-selected-value").val();
                domainValue = -1;
           }
           else if (entitiesThruDomainFlag == 1){
                domainValue = jQuery("#domain-options option:selected").val();
                typeValue = -1;
           }  
           else
           { //get all entities with keyword
               domainValue = -1;
               typeValue = -1; 
           } 
   }
   else if (entitiesThruTypesFlag == 1)
   {
      keywordValue = "";
      //typeValue = jQuery("#type-options option:selected").val();
      typeValue = jQuery("#type-selected-value").val();
      domainValue = -1;
   }
   else if (entitiesThruDomainFlag == 1){
       keywordValue = "";
       typeValue = -1;
       domainValue = jQuery("#domain-options option:selected").val();
   }  
   else
   { //get all entities
        keywordValue = "";
        domainValue = -1;
        typeValue = -1;
   }
   
   getRequest("http://[server_name]/orion_app/getentities?domain="+domainValue+"&windownum="+windowNo+"&windowsize="+entityWindowSize+"&type="+typeValue+"&keyword="+keywordValue, entitiesList);

   if(entitiesList.length < 19){
      jQuery("#entities-options").attr('size',entitiesList.length);}
   else{
   jQuery("#entity-options").attr('size',19);
   }
   if (entitiesList.length <= entityWindowSize){
       jQuery("#entity-next").attr("disabled", true);}
  jQuery("#entity-options").empty();
  for (var i = 0; i < entitiesList.length; i++) {
    jQuery("#entity-options").append('<option value="'+entitiesList[i][0]+'" id="entity-value-1">'+entitiesList[i][1]+'</option>');
  };
   if(entityCount == 1){
      jQuery("#entity-back").attr("disabled",true);}
});
   




jQuery("#type-next").click(function(){
   event.stopPropagation();
   var domainValue;
   var typeValue = -1;
   var keywordValue;
   if (typeCount == 1){
     jQuery("#type-back").attr("disabled",false);}
   typeCount = typeCount + 1;
   var windowNo = typeCount - 1;
   typesList.length = 0;
   
   if (typeKeywordSearchFlag == 1)
   {
           keywordValue = typeKeywordSearch;
           if (typesThruDomainFlag == 1){
                 domainValue = jQuery("#domain-options option:selected").val();
           }  
           else
           { //get all types with keyword
               domainValue = -1;
           } 
   }
   else if (typesThruDomainFlag == 1){
                 keywordValue = "";
                 domainValue = jQuery("#domain-options option:selected").val();
   }  
   else
   {
      keywordValue = "";
      domainValue = -1;
   }
   getRequest("http://[server_name]/orion_app/gettypes?domain="+domainValue+"&windownum="+windowNo+"&windowsize="+typeWindowSize+"&keyword="+keywordValue, typesList);
   if(typesList.length < 19){
      jQuery("#type-options").attr('size',typesList.length);}
   else{
   jQuery("#type-options").attr('size',19);
   }
   if (typesList.length <= typeWindowSize){
       jQuery("#type-next").attr("disabled", true);}
   jQuery("#type-options").empty();
  for (var i = 0; i < typesList.length; i++) {
    jQuery("#type-options").append('<option value="'+typesList[i][0]+'" id="type-value-1">'+typesList[i][1]+'</option>');
  };
   if(typeCount == 1){
      jQuery("#type-back").attr("disabled",true);}
});



jQuery("#entity-next").click(function(){
   event.stopPropagation();
   var domainValue;
   var typeValue;
   var keywordValue;
   if (entityCount == 1){
     jQuery("#entity-back").attr("disabled",false);}
   entityCount = entityCount + 1;
   var windowNo = entityCount - 1;
   entitiesList.length = 0;
   
   if (entityKeywordSearchFlag == 1)
   {
           keywordValue = entityKeywordSearch;
           if (entitiesThruTypesFlag == 1){
                domainValue = -1;
                //typeValue = jQuery("#type-options option:selected").val();
                typeValue = jQuery("#type-selected-value").val();
           }
           else if (entitiesThruDomainFlag == 1){
                 typeValue = -1;
                 domainValue = jQuery("#domain-options option:selected").val();
           }  
           else
           { //get all entities with keyword
                 typeValue = -1;
                 domainValue = -1;
           } 
   }
   else if (entitiesThruTypesFlag == 1)
   {
      //typeValue = jQuery("#type-options option:selected").val();
      typeValue = jQuery("#type-selected-value").val();
      domainValue = -1;
      keywordValue = "";
   }
   else if (entitiesThruDomainFlag == 1){
      domainValue = jQuery("#domain-options option:selected").val();
      typeValue = -1;
      keywordValue = "";
   }  
   else
   {
      domainValue = -1;
      typeValue = -1;
      keywordValue = "";
   }
   getRequest("http://[server_name]/orion_app/getentities?domain="+domainValue+"&windownum="+windowNo+"&windowsize="+entityWindowSize+"&type="+typeValue+"&keyword="+keywordValue, entitiesList);
   if(entitiesList.length < 19){
      jQuery("#entities-options").attr('size',entitiesList.length);}
   else{
   jQuery("#entity-options").attr('size',19);
   }
   if (entitiesList.length <= entityWindowSize){
       jQuery("#entity-next").attr("disabled", true);}
   jQuery("#entity-options").empty();
  for (var i = 0; i < entitiesList.length; i++) {
    jQuery("#entity-options").append('<option value="'+entitiesList[i][0]+'" id="entity-value-1">'+entitiesList[i][1]+'</option>');
  };
   if(entityCount == 1){
      jQuery("#entity-back").attr("disabled",true);}
});
  
   


jQuery("#selectDiv").click(function(){
   jQuery("#entity-options").attr('size', 1);
   jQuery("#type-options").attr('size',1);
});


jQuery("#type-x-button").click(function(){
   entitiesThruTypeFlag = 0; 
   jQuery("#type-selected-value").attr('value',-1);
   jQuery("#type-selected").attr('value',"Select Type...");
   jQuery("#type-selected").hide();
   jQuery("#type-x-button").hide();
   jQuery("#entity-selected-value").attr('value',-1);
   jQuery("#entity-selected").attr('value',"Select Entity...");
   jQuery("#entity-selected").hide();
   jQuery("#entity-x-button").hide();
   if (typesThruDomainFlag == 1)
   {
     addTypesAndEntities(); 
   }else{
     displayTypeOptions(types);}
   if (entitiesThruDomainFlag == 1){  }
    else{
     displayEntityOptions(entities); }
   restart();
});



jQuery("#entity-x-button").click(function(){ 
   jQuery("#entity-selected-value").attr('value',-1);
   jQuery("#entity-selected").attr('value',"Select Entity...");
   jQuery("#entity-selected").hide();
   jQuery("#entity-x-button").hide();
   if (entitiesThruTypesFlag == 1)
   {
     addEntities(); 
   }else if (entitiesThruDomainFlag == 1)
   { addTypesAndEntities();}
    else{
     displayEntityOptions(entities); }
   restart();
});




jQuery("#save-changes").click(function(){
     jQuery("#modal-text").empty();
     if (selected_node && newNode) {
         /*var text2 = jQuery("#entity-options option:selected").text();
         var text1 = jQuery("#type-options option:selected").text();*/

         var text2 = jQuery("#entity-selected").val();
         var text1 = jQuery("#type-selected").val();
         if (text2 != "Select Entity..." ||  text1 != "Select Type..."){
                jQuery("#myModal").modal('hide');
                var entity = 0; var type = 0; var domain = 0;
                if (text1 != "Select Type...") type = 1;
                if (text2 != "Select Entity...") entity = 1;
                var text3 = jQuery("#domain-options option:selected").text();
                if (text3 != "Select Domain...") domain = 1;        
                setNodeDetails( entity, type, domain);
                selected_node = null;
                jQuery("#show-instructions").attr("disabled", false);
                newNode = false;
                // jQuery("#modal-text").empty();
                restart();
                animatedHelp();
                suggestionCounter = 0;
                if(nodes.length == 1) {
                    var mousedownMode = 2;
                    addSuggestions(1,mousedownMode); 
                };
         }else{
            jQuery("#modal-text").append("<p>Please Select Type or Entity to continue</p>");
         }
      }else{
          var text = jQuery("#edge-options option:selected").text();
          if (text == " Select Edge") {
              jQuery("#modal-text").append("<p>Please Select Edge to continue</p>");
          }else{
	  for(var i=0; i<returnObject.length; i++) {
		if(returnObject[i].edge.split("|")[0] == jQuery("#edge-options option:selected").val()) {
		    if(selected_link.source.nodeID != returnObject[i].source.split("|")[0]) {
		       var temp = selected_link.source;
			selected_link.source = selected_link.target;
			selected_link.target = temp;
                        var temp2 = selected_link.actualSourceType;
                        selected_link.actualSourceType = selected_link.actualTargetType;
                        selected_link.actualTargetType = temp2;
		    }
		}
	  }
             jQuery("#myModal").modal('hide');
             var mousedownMode = 2;
             addSuggestions(0,mousedownMode);
             jQuery("#linkId_"+selected_link.id).css('stroke', 'black');
             selected_link = null;
             selected_node = null;
             jQuery("#show-instructions").attr("disabled", false);
             newNode = false;
             newEdge = false;
             // jQuery("#modal-text").empty();
             restart();
             animatedHelp();
             suggestionCounter = 0;
          }
           // jQuery("#myModal").modal('hide');
     }
     /*selected_node = null;
     if (selected_link) {
     jQuery("#linkId_"+selected_link.id).css('stroke', 'black');
     selected_link = null;
     };*/
     checkKeywordStatus();
     restart();
     animatedHelp();
});

function setExamplesFlag(){
   if (getExamplesFlag == 0){
       getExamplesFlag = 1;}
   else {
       getExamplesFlag = 0;}
}


function setMouseOverFlagOfIndex(){
         mouseOverFlagOfIndex = true;
}

function reSetMouseOverFlagOfIndex(){
         mouseOverFlagOfIndex = false;
}


function getRequest(URL, container){
  var returnVar = [];
  var data;
  jQuery.ajax({
      type:"GET",
      beforeSend: function (request)
      {
          request.setRequestHeader("Content-type", "application/json");
          // request.setRequestHeader("Access-Control-Request-Method","GET");
          // request.setRequestHeader("Access-Control-Request-Headers","access-control-allow-origin, accept, content-type");
      },
      url: URL,
      processData: false,
      dataType: "json",
      async:   false,
      success: function(data) {
          if (getExamplesFlag == 0 || mouseOverFlagOfIndex == true){     
               for (var i = 0; i < data.length; i++) {
               var temp = data[i].split(",");
               container[i] = temp;
               var k = 0;
              };
          }
          else{
               var exampleData = {examples: data.examples, isReversible: data.isReversible, objectType: data.objectType, sourceType: data.sourceType};
               container.push(exampleData);
          }
      },
      error: function(){
          container.length = 0;
          data = ["123|Select Name", "234|Domain1", "4345|Domain2", "141|domain3", "3455|Domain4", "2134|Domain5", "7575|Domain6", "7575s|Domain7", "73275|Domain8", "75115|Domain9"];
          for (var i = 0; i < data.length; i++) {
            var temp = data[i].split("|");
            container[i] = temp;
          }
      }
    });
}



function addTypesAndEntities(){
    var domainValue = jQuery("#domain-options option:selected").val();
   // Get all types for the domain
  typeCount = 1;
  var typeValue = -1;
  var keywordValue = "";
  typesThruDomainFlag = 1;
  entitiesThruTypesFlag = 0;
  jQuery("#type-next").attr("disabled",false);
  jQuery("#type-back").attr("disabled",true);
  jQuery("#type-options").attr('size',1);
  var windowNo = typeCount - 1;
  typesList.length = 0;
  getRequest("http://[server_name]/orion_app/gettypes?domain="+domainValue+"&windownum="+windowNo+"&windowsize="+typeWindowSize+"&keyword="+keywordValue, typesList);
  if (typesList.length <= typeWindowSize){
       jQuery("#type-next").attr("disabled", true);}
  jQuery("#type-options").prop('disabled', false);
  jQuery("#type-options").empty();
  for (var i = 0; i < typesList.length; i++) {
    jQuery("#type-options").append('<option value="'+typesList[i][0]+'" id="type-value-1">'+typesList[i][1]+'</option>');
  };

   // Get all entities for the domain
  entityCount = 1;
  entitiesThruDomainFlag = 1;
  jQuery("#entity-next").attr("disabled",false);
  jQuery("#entity-back").attr("disabled",true);
  jQuery("#entity-options").attr('size',1);
  windowNo = entityCount - 1;
  entitiesList.length = 0;
  getRequest("http://[server_name]/orion_app/getentities?domain="+domainValue+"&windownum="+windowNo+"&windowsize="+entityWindowSize+"&type="+typeValue+"&keyword="+keywordValue, entitiesList);
  if (entitiesList.length <= entityWindowSize){
       jQuery("#entity-next").attr("disabled", true);}
  jQuery("#entity-options").prop('disabled', false);
  jQuery("#entity-options").empty();
  for (var i = 0; i < entitiesList.length; i++) {
    jQuery("#entity-options").append('<option value="'+entitiesList[i][0]+'" id="entity-value-1">'+entitiesList[i][1]+'</option>');
  };
}



function addEntities(){
   //var typeValue = jQuery("#type-options option:selected").val();
   var typeValue = jQuery("#type-selected-value").val();
   var domainValue = -1;
   var keywordValue = "";
  entityCount = 1;
  entitiesThruTypesFlag = 1;
  entitiesThruDomainFlag = 0;
  jQuery("#entity-next").attr("disabled",false);
  jQuery("#entity-back").attr("disabled",true);
 // jQuery("#entity-back").style("background-color","red");
  jQuery("#entity-options").attr('size',1);
  var windowNo = entityCount - 1;
  entitiesList.length = 0;
  getRequest("http://[server_name]/orion_app/getentities?domain="+domainValue+"&windownum="+windowNo+"&windowsize="+entityWindowSize+"&type="+typeValue+"&keyword="+keywordValue, entitiesList);
  if (entitiesList.length <= entityWindowSize){
       jQuery("#entity-next").attr("disabled", true);}
  jQuery("#entity-options").empty();
  for (var i = 0; i < entitiesList.length; i++) {
    jQuery("#entity-options").append('<option value="'+entitiesList[i][0]+'" id="entity-value-1">'+entitiesList[i][1]+'</option>');
  };
}
 


function getAllTypes(types){
  typeCount = 1;
  jQuery("#type-next").attr("disabled",false);
  jQuery("#type-back").attr("disabled",true);
  jQuery("#type-options").attr('size',1);
  var windowNo = typeCount - 1;
  types.length = 0;
  var domainValue = -1;
  var keywordValue = "";
  var typeValue = -1;
  getRequest("http://[server_name]/orion_app/gettypes?domain="+domainValue+"&windownum="+windowNo+"&windowsize="+typeWindowSize+"&keyword="+keywordValue, types);
  if (types.length <= typeWindowSize){
       jQuery("#type-next").attr("disabled", true);}
  jQuery("#type-options").empty();
  for (var i = 0; i < entities.length; i++) {
    jQuery("#type-options").append('<option value="'+types[i][0]+'" id="type-value-1">'+types[i][1]+'</option>');
  };
}



function getAllEntities(entities){
  entityCount = 1;
  var keywordValue = "";
   var typeValue = -1;
  var domainValue = -1;
  jQuery("#entity-next").attr("disabled",false);
  jQuery("#entity-back").attr("disabled",true);
  jQuery("#entity-options").attr('size',1);
  var windowNo = entityCount - 1;
  entities.length = 0;
  getRequest("http://[server_name]/orion_app/getentities?domain="+domainValue+"&windownum="+windowNo+"&windowsize="+entityWindowSize+"&type="+typeValue+"&keyword="+keywordValue, entities);
  if (entities.length <= entityWindowSize){
       jQuery("#entity-next").attr("disabled", true);}
  jQuery("#entity-options").empty();
  for (var i = 0; i < entities.length; i++) {
    jQuery("#entity-options").append('<option value="'+entities[i][0]+'" id="entity-value-1">'+entities[i][1]+'</option>');
  };
}

function displayDomainOptions(domain){
  jQuery("#edge-div").hide();
  jQuery("#domain-div").show();
  jQuery("#type-options").empty();
  jQuery("#domain-options").empty();
  jQuery("#keyword-div").show();
  jQuery("#type-keyword-div").show();
  for (var i = 0; i < domain.length; i++) {
    jQuery("#domain-options").append('<option value="'+domain[i][0]+'" id="add-value-1" >'+domain[i][1]+'</option>');
  };
        
}



function displayTypeOptions(types){
   typesThruDomainFlag = 0;
   jQuery("#edge-div").hide();
   jQuery("#type-border").show();
   jQuery("#type-div").show();
   jQuery("#type-options").empty()
   for(var i = 0; i < types.length; i++){
     jQuery("#type-options").append('<option value="'+types[i][0]+'" id="add-value-1" >'+types[i][1]+'</option>');
   };
  typeCount = 1;
  jQuery("#type-next").attr("disabled",false);
  jQuery("#type-back").attr("disabled",true);
  jQuery("#type-options").attr('size',1);
  if (types.length <= typeWindowSize){
       jQuery("#type-next").attr("disabled", true);}
}




function displayEntityOptions(entities){
   entitiesThruTypesFlag = 0;
   entitiesThruDomainFlag = 0;
   jQuery("#edge-div").hide();
   jQuery("#entity-border").show();
   jQuery("#entity-div").show();
   jQuery("#entity-options").empty()
   for(var i = 0; i < entities.length; i++){
     jQuery("#entity-options").append('<option value="'+entities[i][0]+'" id="add-value-1" >'+entities[i][1]+'</option>');
   };
  entityCount = 1;
  jQuery("#entity-next").attr("disabled",false);
  jQuery("#entity-back").attr("disabled",true);
  jQuery("#entity-options").attr('size',1);
  if (entities.length <= entityWindowSize){
       jQuery("#entity-next").attr("disabled", true);}
}



function getDomainStatusForTypeKeyword()
{
    return typesThruDomainFlag;
}

function getDomainStatusForEntityKeyword()
{
    return entitiesThruDomainFlag;
}


function getTypeStatusForEntityKeyword(){
    return entitiesThruTypesFlag;
    
}

function checkConnected(nodes, links){
  if (nodes.length == 1 || nodes.length == 0) {
    return false;
  };
  /*if (nodes.length == 0) {
    return false;
  };
  if (nodes.length == 1) {
    return true;
  };*/

  if (nodes.length == 2 && links.length == 0) {
    return true;
  };
  checkedNodes = [];

  var nodeFound = false;

  for (var i = 0; i < nodes.length; i++) {
    for (var j = 0; j < links.length; j++) {
      var link = links[j];
      if (link.source != link.target) 
        if (nodes[i] == link.source || nodes[i] == link.target) {
          nodeFound = true;
        };
    };
    if (!nodeFound) {
      return true;
    }else{
      nodeFound = false;
    }
  };


  return false;



  // for (ele in links){
  // for (var i = 0; i < links.length; i++) {
  //   var ele = links[i]; 
  //   if (ele.source != ele.target) {
  //     var exist1 = nodes.indexOf(ele.source);
  //     var exist2 = nodes.indexOf(ele.target);
  //     if (exist1 < 0) {
  //       checkedNodes[checkedNodes.length] = ele.source.nodeID;
  //     }
  //     if (exist2 < 0) {
  //       checkedNodes[checkedNodes.length] = ele.target.nodeID; 
  //     }
  //   }
  // }

  // if (checkedNodes.length==0) {
  //   return false;
  // }else{
  //   return true;
  // }
}


function createEntityTextboxList()
{

    t6 = new TextboxList('form_tags_input_5', {unique: true, plugins: {autocomplete: {
                        minLength: 3,
                       queryRemote: true,
                        remote: {url: 'http://[server_name]/orion_app/greeting'}
                         }}});
    t5 = new TextboxList('form_tags_input_4', {unique: true, plugins: {autocomplete: {
                       minLength: 3,
                       queryRemote: true,
                        remote: {url: 'http://[server_name]/orion_app/greeting'}
                         }}});
    t7 = new TextboxList('form_tags_input_6', {unique: true, plugins: {autocomplete: {
                       minLength: 3,
                       queryRemote: true,
                        remote: {url: 'http://[server_name]/orion_app/greeting'}
                         }}});
}
  

function setNodeEditSearchFlag(entitySearch, entitySearchLength)
{
   if (entitySearchLength >= searchLength)
   {
      nodeEditFlag = 1;
      setNodeEditKeywordValue(entitySearch);
   }
   else{
       nodeEditFlag = 0;
       setNodeEditKeywordValue("");
       setPreviousNodeEditValues();
   }
}

function setNodeEditWindowNo(windowNo){
     setWindowNoOfNodeEdit(windowNo);
}


function setTypeSearch(typeSearch, typeSearchLength)
{
   if (typeSearchLength >= searchLength)
   {
      typeKeywordSearchFlag = 1;
      typeKeywordSearch = typeSearch;
   }
   else{
       typeKeywordSearchFlag = 0;
       typeKeywordSearch = null;
       if (typesThruDomainFlag == 0)
       {
           displayAllTheTypeOptions();
       } 
       else if (typesThruDomainFlag == 1){
                 jQuery("#type-options").empty();
                 jQuery("#type-next").attr("disabled",false);
                 jQuery("#type-back").attr("disabled",true);
                 jQuery("#type-options").attr('size',1);
                 if (typesList.length <= typeWindowSize){
                            jQuery("#type-next").attr("disabled", true);}
                 for (var i = 0; i < typesList.length; i++) {
                    jQuery("#type-options").append('<option value="'+typesList[i][0]+'" id="type-value-1">'+typesList[i][1]+'</option>');
                    //jQuery("#type-options").vai = -1; 
                 };
                 
        }
   }
}


function setEntitySearch(entitySearch, entitySearchLength)
{
   if (entitySearchLength >= searchLength)
   {
      entityKeywordSearchFlag = 1;
      entityKeywordSearch = entitySearch;
   }
   else{
       entityKeywordSearchFlag = 0;
       entityKeywordSearch = null;
       if (entitiesThruTypesFlag != 1 && entitiesThruDomainFlag != 1)
       {
       displayAllTheEntityOptions();}
    
     else if (entitiesThruTypesFlag == 1){
                 jQuery("#entity-options").empty();
                  jQuery("#entity-next").attr("disabled",false);
                  jQuery("#entity-back").attr("disabled",true);
                  jQuery("#entity-options").attr('size',1);
                  if (entitiesList.length <= entityWindowSize){
                     jQuery("#entity-next").attr("disabled", true);}
            for (var i = 0; i < entitiesList.length; i++) {
                jQuery("#entity-options").append('<option value="'+entitiesList[i][0]+'" id="entity-value-1">'+entitiesList[i][1]+'</option>');
             };        
         }
     else if (entitiesThruDomainFlag == 1){
                jQuery("#entity-next").attr("disabled",false);
                 jQuery("#entity-back").attr("disabled",true);
                 jQuery("#entity-options").attr('size',1);
                 jQuery("#entity-options").empty();
                 if (entitiesList.length <= entityWindowSize){
                     jQuery("#entity-next").attr("disabled", true);}
         for (var i = 0; i < entitiesList.length; i++) {
                       jQuery("#entity-options").append('<option value="'+entitiesList[i][0]+'" id="entity-value-1">'+entitiesList[i][1]+'</option>');
                  };        
         }
   }
}


function checkKeywordStatus()
{
    if (nodeEditFlag == 1){
        nodeEditFlag = 0;
        t7.list.remove(t7.list.innerText);
        t7 = new TextboxList('form_tags_input_6', {unique: true, plugins: {autocomplete: {
                       minLength: 3,
                       queryRemote: true,
                        remote: {url: 'http://[server_name]/orion_app/greeting'}
                         }}}); 
        resetEditNodeFlag();
    }
    else{
        if (typeKeywordSearchFlag == 1){
            typeKeywordSearchFlag = 0;
            t6.list.remove(t6.list.innerText);
            t6 = new TextboxList('form_tags_input_5', {unique: true, plugins: {autocomplete: {
                       minLength: 3,
                       queryRemote: true,
                        remote: {url: 'http://[server_name]/orion_app/greeting'}
                         }}});

        }
        if (entityKeywordSearchFlag == 1){
            entityKeywordSearchFlag = 0;
            t5.list.remove(t5.list.innerText);
            t5 = new TextboxList('form_tags_input_4', {unique: true, plugins: {autocomplete: {
                       minLength: 3,
                       queryRemote: true,
                        remote: {url: 'http://[server_name]/orion_app/greeting'}
                         }}});

        }
        resetTypeOrEntitySearchFlag();
    }
}



function setKeywordTypeCount(count)
{
   typeCount = count;
}


function setKeywordEntityCount(count)
{
   entityCount = count;
}


