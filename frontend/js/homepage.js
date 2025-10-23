var urlSuffix = window.location.pathname.split('/')[1];
var systemName = urlSuffix.startsWith('orion-n') ? 'naive' : (urlSuffix.startsWith('orion-b') ? 'baseline' : 'orion');
var version = urlSuffix.replace('orion-n', '').replace('orion-b', '').replace('orion', '');

if(systemName === "orion" && version === "") {
    jQuery("#infoContainer-index").show();
    jQuery("#title-caption").show();
}