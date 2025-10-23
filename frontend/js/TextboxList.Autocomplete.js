var entityKeywordWindowSize = 100;
var typeKeywordWindowSize = 100;
var nodeEditKeywordWindowSize = 100;
var keywordEntityCount = 0;
var keywordTypeCount = 0;
var nodeEditWindowNo = 0;
var curEleInTBL = 0;
var nodeEditOptionsSize = 19;
var entityOptionsSize = 19;
var typeOptionsSize = 19;
var typeOrEntitySearchFlag = 0;
var nodeEditFlag = 0;
var entityDefaultSize = 20;
var typesDefaultSize = 20;
(function() {

    TextboxList.Autocomplete = new Class({

        Implements: Options,

        options: {
            minLength: 1,
            maxResults: 0,
            insensitive: true,
            highlight: true,
            highlightSelector: null,
            mouseInteraction: true,
            onlyFromValues: false,
            queryRemote: false,
            remote: {
                url: 'https://" + urlFull + "/orion_app/greeting',
                param: 'search',
                extraParams: {},
                loadPlaceholder: 'Please wait...'
            },
            method: 'standard',

        },

        initialize: function(textboxlist, options) {
            this.setOptions(options);
            this.textboxlist = textboxlist;
            this.textboxlist.addEvent('bitEditableAdd', this.setupBit.bind(this), true)
                .addEvent('bitEditableFocus', this.search.bind(this), true)
                .addEvent('bitEditableBlur', this.hide.bind(this), true)
                .setOptions({
                    bitsOptions: {
                        editable: {
                            addKeys: [],
                            stopEnter: false
                        }
                    }
                });
            if (Browser.Engine.trident) this.textboxlist.setOptions({
                bitsOptions: {
                    editable: {
                        addOnBlur: false
                    }
                }
            });
            if (this.textboxlist.options.unique) {
                this.index = [];
                this.textboxlist.addEvent('bitBoxRemove', function(bit) {
                    if (bit.autoValue) this.index.erase(bit.autoValue);
                }.bind(this), true);
            }
            this.prefix = this.textboxlist.options.prefix + '-autocomplete';
            this.method = TextboxList.Autocomplete.Methods[this.options.method];
            this.container = new Element('div', {
                'class': this.prefix
            }).setStyle('width', this.textboxlist.container.getStyle('width')).inject(this.textboxlist.container);
            if ($chk(this.options.placeholder) || this.options.queryServer)
                this.placeholder = new Element('div', {
                    'class': this.prefix + '-placeholder'
                }).inject(this.container);
            this.list = new Element('ul', {
                'class': this.prefix + '-results'
            }).inject(this.container);
            this.list.addEvent('click', function(ev) {
                ev.stop();
            });
            this.values = this.results = this.searchValues = [];
            this.navigate = this.navigate.bind(this);
        },

        setValues: function(values) {
            this.values = values;
        },

        setupBit: function(bit) {
            bit.element.addEvent('keydown', this.navigate, true).addEvent('keyup', function() {
                this.search();
            }.bind(this), true);
        },

        search: function(bit) {
            // jQuery("#domain-div").show();

            event.stopPropagation();
            var id = this.textboxlist.original.id;
            if (id == "form_tags_input_4") {
                typeOrEntitySearchFlag = 2; // Entity Keyword Search
                jQuery("#entity-options").attr('size', entityDefaultSize);
            } else if (id == "form_tags_input_5") {
                typeOrEntitySearchFlag = 1; // Type Keyword Search
                jQuery("#type-options").attr('size', typesDefaultSize);
            } else if (id == "form_tags_input_6") {
                nodeEditFlag = 1; // Node Edit Entity Keyword Search
                entityEditSearch = 1;
                jQuery("#node-edit-entity-options").attr('size', nodeEditOptionsSize);
            } else if (id == "form_tags_input_7") {
                typeEditSearch = 1;
                jQuery("#node-edit-type-options").attr('size', nodeEditOptionsSize);
            }
            if (bit) this.currentInput = bit;
            if (!this.options.queryRemote && !this.values.length) return;
            var search = this.currentInput.getValue()[1];
            if (entityEditSearch == 1) {
                setNodeEditEntitySearchFlag(search, search.length);
            } else if (typeEditSearch == 1) {
                setNodeEditTypeSearchFlag(search, search.length);
            } else if (typeOrEntitySearchFlag == 1) {
                setTypeSearch(search, search.length);
            } else if (typeOrEntitySearchFlag == 2) {
                setEntitySearch(search, search.length);
            } else {}
            if (search.length < this.options.minLength && curEleInTBL == 0) this.showPlaceholder(this.options.placeholder);
            if (search == this.currentSearch) return;
            this.currentSearch = search;
            this.list.setStyle('display', 'none');
            if (search.length < this.options.minLength) return;
            if (curEleInTBL == 1) return;
            if (this.options.queryRemote) {
                if (nodeEditFlag == 1) {
                    jQuery("#node-edit-entity-next").attr("disabled", false);
                    jQuery("#node-edit-entity-back").attr("disabled", true);
                } else if (typeOrEntitySearchFlag == 1) {
                    jQuery("#type-next").attr("disabled", false);
                    jQuery("#type-back").attr("disabled", true);
                } else if (typeOrEntitySearchFlag == 2) {
                    jQuery("#entity-next").attr("disabled", false);
                    jQuery("#entity-back").attr("disabled", true);
                } else {}
                /*if (this.searchValues[search]){
			   this.values = this.searchValues[search];
                           var stored = this.values;
                           if (nodeEditFlag == 1){
                               if(stored.length >= 19){
                                  nodeEditOptionsSize = 19;
                                  jQuery("#node-edit-entity-options").attr('size',19);
                                }else{
                                 nodeEditOptionsSize = stored.length;
                                 jQuery("#node-edit-entity-options").attr('size',nodeEditOptionsSize);}
                                setNodeEditWindowNo(nodeEditWindowNo);
                                if (stored.length <= nodeEditKeywordWindowSize){
                                     jQuery("#node-edit-entity-next").attr("disabled", true);}
                                jQuery("#node-edit-entity-options").empty();
                                for (var i = 0; i < stored.length; i++) {
                                     jQuery("#node-edit-entity-options").append('<option value="'+stored[i][0]+'" id="type-value-1">'+stored[i][1]+'</option>');
                                };
                           }
                           else if (typeOrEntitySearchFlag == 1){
                               if(stored.length >= 19){
                                  typeOptionsSize = 19;
                                  jQuery("#type-options").attr('size',19);
                                }else{
                                 typeOptionsSize = stored.length;
                                 jQuery("#type-options").attr('size',typeOptionsSize);}

                                setKeywordTypeCount(keywordTypeCount+1);
                                if (stored.length <= typeKeywordWindowSize){
                                jQuery("#type-next").attr("disabled", true);}
                                jQuery("#type-options").empty();



                                 for (var i = 0; i < stored.length; i++) {
                                     jQuery("#type-options").append('<option value="'+stored[i][0]+'" id="type-value-1">'+stored[i][1]+'</option>');
                                };

                           }
                           else if (typeOrEntitySearchFlag == 2){
                               if(stored.length >= 19){
                                  jQuery("#entity-options").attr('size',19);entityOptionsSize = 19;
                                }else{
                                 entityOptionsSize = stored.length;
                                 jQuery("#entity-options").attr('size',entityOptionsSize);}

                                setKeywordEntityCount(keywordEntityCount+1);
                                if (stored.length <= entityKeywordWindowSize){
                                jQuery("#entity-next").attr("disabled", true);}
                                jQuery("#entity-options").empty();



                                 for (var i = 0; i < stored.length; i++) {
                                     jQuery("#entity-options").append('<option value="'+stored[i][0]+'" id="entity-value-1">'+stored[i][1]+'</option>');
                                };
                          }else{}
			} else {*/
                var windownum;
                var domain;
                var type;
                var domainValue;
                var typeValue;
                var path;
                var windowSize;
                if (entityEditSearch == 1) {
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


                    var nodeToBeedited = getNodeToBeEdited();
                    var nodeEditEdges = getNodeEditEdges();
                    var editNodeKeyword = search;
                    var text = jQuery("#edit-type-selected").text();
                    var value = jQuery("#edit-type-selected-value").val();
                    var domainVal = jQuery("#node-edit-domain-options option:selected").val();
                    if (!domainVal) {
                        domainVal = -1;
                    }
                    if (!value) {
                        value = -1;
                    }
                    if ((text.toLowerCase() != "select type..." && text != "") || domainVal != -1 && domainVal != -1) {
                        path = "https://" + urlFull + "/orion_app/getentities?domain=" + domainVal + "&windownum=0&windowsize=100&type=" + value + "&keyword=" + editNodeKeyword;
                        // 								path = "https://" + urlFull + "/orion_app/geteditentity?typeid="+nodeToBeEdited.type+"&entityid="+nodeToBeEdited.nodeID+"&entityname="+nodeToBeEdited.tempName+"&windownum="+nodeEditWindowNo+"&windowsize="+windowSize+"&edges="+nodeEditEdges+"&keyword="+editNodeKeyword;
                    } else if (nodeToBeEdited.entity != -1) {
                        path = "https://" + urlFull + "/orion_app/geteditentity?typeid=" + nodeToBeEdited.type + "&entityid=" + nodeToBeEdited.nodeID + "&entityname=" + nodeToBeEdited.tempName + "&windownum=" + nodeEditWindowNo + "&windowsize=100&edges=" + nodeEditEdges + "&keyword=" + editNodeKeyword;
                    } else {
                        path = "https://" + urlFull + "/orion_app/getentities?domain=" + domainVal + "&windownum=0&windowsize=100&type=" + nodeToBeEdited.nodeID + "&keyword=" + editNodeKeyword;
                    }

                    if (candEdges.length == 0) {
                        path = "https://" + urlFull + "/orion_app/getentities?domain=" + domainVal + "&windownum=0&windowsize=100&type=" + value + "&keyword=" + editNodeKeyword;
                    }

                } else if (typeEditSearch == 1) {

                    var candEdges = "";
                    var typesOptionList = "";
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
                    var domainVal = jQuery("#edit-domain-selected-value").val();
                    if (!domainVal) {
                        domainVal = -1;
                    }



                    path = "https://" + urlFull + "/orion_app/getnodetypecandidate?edges=" + candEdges + "&domain=" + domainVal + "&keyword=" + search;
                    if (candEdges.length == 0) {
                        //     url = "https://" + urlFull + "/orion_app/geteditnode?node="+nodeToBeEdited.nodeID+"&windownum="+0+"&windowsize="+windowSize+"&edges="+nodeEditEdges+"&keyword="+editNodeKeyword+"&domain="+domainVal;
                        path = "https://" + urlFull + "/orion_app/gettypes?domain=" + domainVal + "&windownum=0&windowsize=100&keyword=" + search;
                    }

                } else if (typeOrEntitySearchFlag == 1) {
                    if(curTypeList.concat(curEndTypeList).length != 0) {
                      updateDomainsTypesEntities(search, "");
                      displayTypeOptions(generatedTypes);
                    } else {
                      domain = getDomainStatusForTypeKeyword();
                      windownum = keywordTypeCount;
                      windowSize = typeKeywordWindowSize;
                      typeValue = -1;
                      if (domain == 0) { // all types based on keyword
                          domainValue = -1;
                      } else { // types  based on domain and keyword
                          domainValue = jQuery("#domain-options option:selected").val();
                      }

                      path = "https://" + urlFull + "/orion_app/gettypes?domain=" + domainValue + "&windownum=" + windownum + "&windowsize=" + windowSize + "&keyword=" + search;
                    }
                } else if (typeOrEntitySearchFlag == 2) {
                    if(curTypeList.concat(curEndTypeList).length != 0) {
                      updateDomainsTypesEntities("", search);
                      displayEntityOptions(generatedEntities);
                    } else {
                      domain = getDomainStatusForEntityKeyword();
                      type = getTypeStatusForEntityKeyword();
                      windownum = keywordEntityCount;
                      windowSize = entityKeywordWindowSize;
                      if (domain == 0 && type == 0) { // All entities based on keyword
                          domainValue = -1;
                          typeValue = -1;
                      } else if (domain == 1) { // Entities based on domain and keyword
                          domainValue = jQuery("#domain-options option:selected").val();
                          typeValue = -1;
                      } else if (type == 1) {
                          typeValue = jQuery("#type-options option:selected").val();
                          domainValue = -1;
                          // Entities based on type and keyword
                      } else {}
                      path = "https://" + urlFull + "/orion_app/getentities?domain=" + domainValue + "&windownum=" + windownum + "&windowsize=" + windowSize + "&type=" + typeValue + "&keyword=" + search;
                    }
                }
                var data = this.options.remote.extraParams,
                    that = this;
                var data1 = search;
                var temp = [];
                var returnVar = [];
                jQuery.ajax({
                    type: "GET",
                    beforeSend: function(request) {
                        request.setRequestHeader("Content-type", "application/json");
                    },
                    url: path,
                    processData: false,
                    dataType: "json",
                    async: false,
                    success: function(data1) {
                        for (var i = 0; i < data1.length; i++) {
                            temp[i] = data1[i].split(",");
                        };
                        that.searchValues[search] = temp;
                        that.values = temp;
                        if (entityEditSearch == 1) {
                            jQuery("#node-edit-save-changes").css("background-color", "#828b8f");
                            jQuery("#node-edit-entity-back").attr("disabled", true);
                            jQuery("#node-edit-entity-next").attr("disabled", false);
                            setNodeEditWindowNo(nodeEditWindowNo);
                            if (temp.length <= nodeEditKeywordWindowSize) {
                                jQuery("#node-edit-entity-next").attr("disabled", true);
                            }
                            jQuery("#node-edit-entity-options").empty();
                            if (temp.length < 19) {

                                jQuery("#node-edit-entity-options").attr('size', 19);
                                //                                                 nodeEditOptionsSize = temp.length;
                            } else {
                                nodeEditOptionsSize = 19;
                            }

                            // for (var i = 0; i < temp.length; i++) {
                            //      jQuery("#node-edit-entity-options").append('<option value="'+temp[i][0]+'" id="type-value-1">'+temp[i][1]+'</option>');
                            // };
                            for (var i = 0; i < temp.length; i++) {
                                if (temp[i][temp[i].length - 1] == "preview") {
                                    jQuery("#node-edit-entity-options").append('<option value="' + temp[i][0] + '" id="type-value-1" data-html="true" data-toggle="tooltip" data-container="#tooltip_container">' + temp[i].slice(1, temp[i].length - 1).join() + '</option>'); //Changed by Heet
                                } else {

                                    jQuery("#node-edit-entity-options").append('<option value="' + temp[i][0] + '" id="type-value-1" >' + temp[i].slice(1, temp[i].length - 1).join() + '</option>'); //Changed by Heet
                                }
                            };
                            jQuery('#tooltip_container').remove();
                            jQuery('<div id="tooltip_container"></div>').insertAfter("#node-edit-entity-options");
                        } else if (typeEditSearch == 1) {
                            setNodeEditWindowNo(nodeEditWindowNo);
                            if (temp.length <= nodeEditKeywordWindowSize) {
                                jQuery("#node-edit-type-next").attr("disabled", true);
                            }
                            jQuery("#node-edit-type-options").empty();
                            for (var i = 0; i < temp.length; i++) {
                                jQuery("#node-edit-type-options").append('<option value="' + temp[i][0] + '" id="add-value-1">' + temp[i][1].toUpperCase() + '</option>');
                            }
                            jQuery('#tooltip_container').remove();
                            jQuery('<div id="tooltip_container"></div>').insertAfter("#node-edit-type-options");
                        } else if (typeOrEntitySearchFlag == 1) {
                            if(curTypeList.concat(curEndTypeList).length == 0) {
                              setKeywordTypeCount(keywordTypeCount + 1);
                              if (temp.length <= typeKeywordWindowSize) {
                                  jQuery("#type-next").attr("disabled", true);
                              }
                              jQuery("#type-options").empty();
                              if (temp.length < 19) {
                                  jQuery("#type-options").attr('size', typesDefaultSize);
                                  typeOptionsSize = temp.length;
                              } else {
                                  typeOptionsSize = 19;
                              }

                              for (var i = 0; i < temp.length; i++) {
                                  jQuery("#type-options").append('<option value="' + temp[i][0] + '" id="type-value-1" data-html="true">' + temp[i][1] + '</option>');
                              };
                              jQuery('#tooltip_container').remove();
                              jQuery('<div id="tooltip_container"></div>').insertAfter("#entity-options");
                            }
                        } else if (typeOrEntitySearchFlag == 2) {
                            if(curTypeList.concat(curEndTypeList).length == 0) {
                              setKeywordEntityCount(keywordEntityCount + 1);
                              if (temp.length <= entityKeywordWindowSize) {
                                  jQuery("#entity-next").attr("disabled", true);
                              }
                              jQuery("#entity-options").empty();
                              if (temp.length < 19) {
                                  jQuery("#entity-options").attr('size', entityDefaultSize);
                                  entityOptionsSize = temp.length;
                              } else {
                                  entityOptionsSize = 19;
                              }

                              for (var i = 0; i < temp.length; i++) {
                                  if (temp[i][temp[i].length - 1] == "preview") {
                                      jQuery("#entity-options").append('<option value="' + temp[i][0] + '" id="entity-value-1" data-html="true" data-toggle="tooltip" data-container="#tooltip_container">' + temp[i].slice(1, temp[i].length - 1).join() + '</option>'); //Changed by Heet
                                  } else {

                                      jQuery("#entity-options").append('<option value="' + temp[i][0] + '" id="entity-value-1" >' + temp[i].slice(1, temp[i].length - 1).join() + '</option>'); //Changed by Heet
                                  }
                              };
                              jQuery('#tooltip_container').remove();
                              jQuery('<div id="tooltip_container"></div>').insertAfter("#entity-options");
                            }
                        }

                        //that.ShowResults(search);
                    },
                    error: function() {
                        data1 = ["123|Select Name", "456|Keyword1", "789|Keyword2", "567|Keyword3"];
                        for (var i = 0; i < data1.length; i++) {
                            temp[i] = data1[i].split("|");
                        };
                        that.searchValues[search] = temp;
                        that.values = temp;
                        //that.showResults(search);
                        //jQuery("#domain-div").show();
                    }
                });



                /*var data = this.options.remote.extraParams, that = this;
                if ($type(data) == 'function') data = data.run([], this);
                data[this.options.remote.param] = search;
                if (this.currentRequest) this.currentRequest.cancel();
                this.currentRequest = new Request.JSON({url: this.options.remote.url, data: data, onRequest: function(){
                	that.showPlaceholder(that.options.remote.loadPlaceholder);
                }, onSuccess: function(data){
                	that.searchValues[search] = data;
                	that.values = data;
                	that.showResults(search);
                }}).send();*/
                //}
            }
            if (this.values.length) this.showResults(search);
        },

        showResults: function(search) {
            //jQuery("#domain-div").hide();
            var results = this.method.filter(this.values, search, this.options.insensitive, this.options.maxResults);
            if (this.index) results = results.filter(function(v) {
                return !this.index.contains(v);
            }, this);
            this.hidePlaceholder();
            if (!results.length) return;
            this.blur();
            this.list.empty().setStyle('display', 'block');
            results.each(function(r) {
                this.addResult(r, search);
            }, this);
            if (this.options.onlyFromValues) this.focusFirst();
            this.results = results;
        },

        addResult: function(r, search) {
            var element = new Element('li', {
                'class': this.prefix + '-result',
                'html': $pick(r[3], r[1])
            }).store('textboxlist:auto:value', r);
            this.list.adopt(element);
            if (this.options.highlight) $$(this.options.highlightSelector ? element.getElements(this.options.highlightSelector) : element).each(function(el) {
                if (el.get('html')) this.method.highlight(el, search, this.options.insensitive, this.prefix + '-highlight');
            }, this);
            if (this.options.mouseInteraction) {
                element.setStyle('cursor', 'pointer').addEvents({
                    mouseenter: function() {
                        this.focus(element);
                    }.bind(this),
                    mousedown: function(ev) {
                        ev.stop();
                        $clear(this.hidetimer);
                        this.doAdd = true;
                    }.bind(this),
                    mouseup: function() {
                        if (this.doAdd) {
                            this.addCurrent();
                            this.currentInput.focus();
                            this.search();
                            this.doAdd = false;
                        }
                    }.bind(this)
                });
                if (!this.options.onlyFromValues) element.addEvent('mouseleave', function() {
                    if (this.current == element) this.blur();
                }.bind(this));
            }
        },

        hide: function(ev) {
            this.hidetimer = (function() {
                this.hidePlaceholder();
                this.list.setStyle('display', 'none');
                this.currentSearch = null;
            }).delay(Browser.Engine.trident ? 150 : 0, this);
        },

        showPlaceholder: function(customHTML) {
            if (this.placeholder) {
                this.placeholder.setStyle('display', 'block');
                if (customHTML) this.placeholder.set('html', customHTML);
            }
        },

        hidePlaceholder: function() {
            if (this.placeholder) this.placeholder.setStyle('display', 'none');
        },

        focus: function(element) {
            if (!element) return this;
            this.blur();
            this.current = element.addClass(this.prefix + '-result-focus');
        },

        blur: function() {
            if (this.current) {
                this.current.removeClass(this.prefix + '-result-focus');
                this.current = null;
            }
        },

        focusFirst: function() {
            return this.focus(this.list.getFirst());
        },

        focusRelative: function(dir) {
            if (!this.current) return this;
            return this.focus(this.current['get' + dir.capitalize()]());
        },

        addCurrent: function() {
            var value = this.current.retrieve('textboxlist:auto:value');
            var b = this.textboxlist.create('box', value.slice(0, 3));
            curEleInTBL++;
            // jQuery("#domain-div").show();
            if (b) {
                b.autoValue = value;
                if (this.index != null) this.index.push(value);
                this.currentInput.setValue([null, '', null]);
                b.inject($(this.currentInput), 'before');

                keywordUpdate(value);



                //jQuery("#type-div").show();
                //jQuery("#entity-div").show();
                //jQuery("#domain-options").empty();
                //jQuery("#type-options").empty();
                //jQuery("#entity-options").empty();
                //var path = "http://" + urlFull + "/orion_app/gettypes?name=-38";//+value[0];
                //var data1 = value[0];
                //var temp=[];
                //var returnVar=[];
                //jQuery.ajax({
                //type:"GET",
                //beforeSend: function (request)
                // {
                //   request.setRequestHeader("Content-type", "application/json");
                //},
                //url: path,
                //processData: false,
                //dataType:"json",
                //async: false,
                //success: function(data1) {
                //	for (var i =0; i < data1.length; i++){
                //	temp[i] = data1[i].split("|");
                //	};
                //     jQuery("#domain-options").append('<option value="'+temp[1][0]+'" id="add-value-1">'+temp[1][1]+'</option>');
                //       jQuery("#type-options").append('<option value="'+temp[2][0]+'" id="add-value-1">'+temp[2][1]+'</option>');
                //       jQuery("#entity-options").append('<option value="'+temp[3][0]+'" id="add-value-1">'+temp[3][1]+'</option>');
                //       var noOfDomainOptions = 1;
                //       var noOfTypeOptions = 1;
                //       var noOfEntityOptions = 1;
                //       var textDomain = jQuery("#domain-options option:selected").text();
                //       var textType = jQuery("#edge-options option:selected").text();
                //       var textEntity = jQuery("#entity-options option:selected").text();
                //       if (textEntity != ""){
                //            var val = 3;   //Entity
                //            keywordUpdate(val);
                //       }
                //       else{
                //            var val = 2;   //Type
                //            keywordUpdate(val);
                //       }
                //
                //	},
                //	error: function(){
                //      data1 = ["123|Domain1", "456|Type1", "789|Entity1"];
                //    for(var i=0; i< data1.length; i++)
                //  {
                //    temp[i] = data1[i].split("|");
                // };
                //  jQuery("#domain-options").append('<option value="'+temp[0][0]+'" id="add-value-1">'+temp[0][1]+'</option>');
                //  jQuery("#type-options").append('<option value="'+temp[1][0]+'" id="add-value-1">'+temp[1][1]+'</option>');
                //  jQuery("#entity-options").append('<option value="'+temp[2][0]+'" id="add-value-1">'+temp[2][1]+'</option>');
                // }});




            }
            this.blur();
            return this;
        },

        navigate: function(ev) {
            switch (ev.code) {
                case Event.Keys.up:
                    ev.stop();
                    (!this.options.onlyFromValues && this.current && this.current == this.list.getFirst()) ? this.blur(): this.focusRelative('previous');
                    break;
                case Event.Keys.down:
                    ev.stop();
                    this.current ? this.focusRelative('next') : this.focusFirst();
                    break;
                case Event.Keys.enter:
                    ev.stop();
                    //if (this.current) this.addCurrent();
                    //else if (!this.options.onlyFromValues){
                    //	var value = this.currentInput.getValue();
                    //	var b = this.textboxlist.create('box', value);
                    //	if (b){
                    //		b.inject($(this.currentInput), 'before');
                    //		this.currentInput.setValue([null, '', null]);
                    //	}
                    //	}
            }
        }

    });

    TextboxList.Autocomplete.Methods = {

        standard: {
            filter: function(values, search, insensitive, max) {
                var newvals = [],
                    regexp = new RegExp('\\b' + search.escapeRegExp(), insensitive ? 'i' : '');
                for (var i = 0; i < values.length; i++) {
                    if (newvals.length === max) break;
                    if (values[i][1].test(regexp)) newvals.push(values[i]);
                }
                return newvals;
            },

            highlight: function(element, search, insensitive, klass) {
                var regex = new RegExp('(<[^>]*>)|(\\b' + search.escapeRegExp() + ')', insensitive ? 'ig' : 'g');
                return element.set('html', element.get('html').replace(regex, function(a, b, c) {
                    return (a.charAt(0) == '<') ? a : '<strong class="' + klass + '">' + c + '</strong>';
                }));
            }
        }

    };



})();


function DecrementCurEleNo() {
    curEleInTBL--;
}

function resetTypeOrEntitySearchFlag() {
    typeOrEntitySearchFlag = 0;
}


function resetTypeEditNodeFlag() {
    typeEditSearch = 0;
}

function resetEditNodeFlag() {
    nodeEditFlag = 0;
    entityEditSearch = 0;
}

function fetchWiki(target) {
    let tooltipText = "d";

    jQuery.ajax({
        url: "https://xxx.uta.edu/orion_app/getwikisummary?nodeid=" + target.value,
        type: 'get',
        async: false,
        success: function(response) {
            jQuery(target).attr('data-original-title', response);
        }
    });
    return tooltipText;
}



jQuery('body').tooltip({
    delay: 500,
    placement: "bottom",
    title: fetchWiki("12333"),
    html: true,
    selector: "[data-toggle='tooltip']"
});



jQuery(document).on("show.bs.tooltip", "[data-toggle='tooltip']", function(el) {
    fetchWiki(el.target);
});
