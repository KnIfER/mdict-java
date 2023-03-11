/**这个不是网页版*/
var w=window, d=document;
var LoadMark, frameAt;
window.debug=function(){var e=arguments,s=['fatal web ::'];for(var i=0;i<e.length;i++)s.push(e[i]);console.log(s)};
var _log = debug;
//function _log(...e){console.log('fatal web::'+e)};
w.addEventListener('load',function(e){
    d.body.contentEditable=!1;
    _highlight(null);
    var lnks = document.links;
	for(var i=0,max=Math.min(lnks.length,10);i<max;i++) {
		if(lnks[i].href.startsWith("sound")) {
			lnks=1; break;
		}
	} 
	app.maySound(sid.get(), lnks===1);
    _log('mdx::wrappedOnLoadFunc...');
},false);
function wrappedFscrFunc(e){
	//_log('begin fullscreen!!! wrappedFscrFunc');
	e = e.target;
	if(app)e.webkitDisplayingFullscreen?app.onRequestFView(e.videoWidth, e.videoHeight)
		:app.onExitFView()
}
w.addEventListener('fullscreenchange', wrappedFscrFunc);
w.addEventListener('webkitfullscreenchange', wrappedFscrFunc);
w.addEventListener('mozfullscreenchange', wrappedFscrFunc);
function _highlight(keyword){
    var b1=keyword==null;
    if(b1)
        keyword=app.getCurrentPageKey(sid.get());
    if(keyword==null||b1&&keyword.trim().length==0)
        return;
    if(!LoadMark) {
        function cb(){LoadMark=1;highlight(keyword);}
        try{loadJs('//mdbr/markloader.js', cb)}catch(e){w.loadJsCb=cb;app.loadJs(sid.get(),'markloader.js');}
    } else highlight(keyword);
}
w.addEventListener('touchstart',function(e){
    //_log('fatal wrappedOnDownFunc');
    if(!w._touchtarget_lck && e.touches.length==1){
        w._touchtarget = e.touches[0].target;
    }
    //_log('fatal wrappedOnDownFunc' +w._touchtarget);
});
function loadJs(url,callback){
    var script=d.createElement('script');
    script.type="text/javascript";
    if(typeof(callback)!="undefined"){
        script.onload=function(){
            callback();
        }
    }
    script.src=url;
    d.body.appendChild(script);
}
window.expUrl = function() {
	return bid+"_"+_posid;
}