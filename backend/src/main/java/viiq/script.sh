#!/bin/sh

sed -i 's/import clientServer/import viiq.clientServer/g' *
sed -i 's/import commons/import viiq.commons/g' *
sed -i 's/import decisionForest/import viiq.decisionForest/g' *
sed -i 's/import graphCompletionGuiMain/import viiq.graphCompletionGuiMain/g' *
sed -i 's/import graphQuerySuggestionMain/import viiq.graphQuerySuggestionMain/g' *
sed -i 's/import randomEdgeSuggestion/import viiq.randomEdgeSuggestion/g' *
sed -i 's/import utils/import viiq.utils/g' *
sed -i 's/import randomForest/import viiq.randomForest/g' *
sed -i 's/import naiveBayesian/import viiq.naiveBayesian/g' *
sed -i 's/import prepareTrainingData/import viiq.prepareTrainingData/g' *
sed -i 's/import barcelonaToFreebase/import viiq.barcelonaToFreebase/g' *
sed -i 's/import barcelonaCorpus/import viiq.barcelonaCorpus/g' *
sed -i 's/package /package viiq./g' *
