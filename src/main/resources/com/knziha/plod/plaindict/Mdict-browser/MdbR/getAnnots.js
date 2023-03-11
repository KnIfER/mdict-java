
debug('getNotesAdapter()');

var ret=[], rootNode=document.body, p=rootNode._pd_ref;

if(p) {
    for(var i=0,n;n=p.children[i++];) {
        if(n.nid!=undefined) {
            ret.push(n.children[1].innerText);
            ret.push(n.nid+'');
        }
    }
}

ret



