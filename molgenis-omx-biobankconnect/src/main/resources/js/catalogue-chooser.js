(function($, molgenis) {
	"use strict";
	
	var restApi = new molgenis.RestClient();
	var searchApi = new molgenis.SearchClient();
	
	molgenis.CatalogueChooser = function OntologyAnnotator(){};
	
	molgenis.CatalogueChooser.prototype.changeDataSet = function(selectedDataSetId){
		if(selectedDataSetId !== null && selectedDataSetId !== '' && selectedDataSetId !== undefined){
			var dataSetEntity = restApi.get('/api/v1/dataset/' + selectedDataSetId, {'expand' : ['ProtocolUsed']});
			$('#selected-catalogue').empty().append(dataSetEntity.Name);
			var request = {
				documentType : 'protocolTree-' + molgenis.hrefToId(dataSetEntity.ProtocolUsed.href),
				query:{
					rules:[[{
						field : 'type',
						operator : 'EQUALS',
						value : 'observablefeature'
					}]]
				}
			};
			searchApi.search(request, function(searchResponse){
				$('#catalogue-name').empty().append(dataSetEntity.Name);
				$('#dataitem-number').empty().append(searchResponse.totalHitCount);
				updateMatrix(molgenis.hrefToId(dataSetEntity.ProtocolUsed.href));
				initSearchDataItems(dataSetEntity);
			});
		}else{
			$('#catalogue-name').empty().append('Nothing selected');
			$('#dataitem-number').empty().append('Nothing selected');
		}
		
		function initSearchDataItems(dataSet) {
			$('#search-dataitem').typeahead({
				source: function(query, process) {
					molgenis.dataItemsTypeahead('observablefeature', molgenis.hrefToId(dataSet.href), query, process);
				},
				minLength : 3,
				items : 20
			}).on('keydown', function(e){
			    if (e.which == 13) {
			    	$('#search-button').click();
			    	return false;
			    }
			}).on('keyup', function(e){
				if($(this).val() === ''){
					updateMatrix(molgenis.hrefToId(dataSet.ProtocolUsed.href));
			    }
			});
			$('#search-button').click(function(){
				updateMatrix(molgenis.hrefToId(dataSet.ProtocolUsed.href));
			});
		}
	};
	
	function updateMatrix(protocolId){
		molgenis.createMatrixForDataItems({
			'dataSetId' : protocolId,
			'tableHeaders' : ['Name', 'Description'],
			'queryText' : $('#search-dataitem').val(),
			'sortRule' : null,
			'createTableRow' : createTableRow,
			'container' : $('#container')
		});
	}
	
	function createTableRow(feature){
		var row = $('<tr />');
		var description = feature.description;
		var isPopOver = description.length < 120;
		var descriptionSpan = $('<span />').html(isPopOver ? description : description.substring(0, 120) + '...');
		if(!isPopOver){
			descriptionSpan.addClass('show-popover');
			descriptionSpan.popover({
				content : description,
				trigger : 'hover',
				placement : 'bottom'
			});
		}
		
		var featureNameSpan = $('<span>' + feature.name + '</span>');
		$('<td />').append(featureNameSpan).appendTo(row);
		$('<td />').append(descriptionSpan).appendTo(row);
		return row;
	}
	
}($, window.top.molgenis = window.top.molgenis || {}));
