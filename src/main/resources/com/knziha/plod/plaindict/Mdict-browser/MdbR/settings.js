function initSettings(w,d,cs){
    (function(){
        var style/**/; 
        if(false) {
            // 把css内联编译进来
            style/**/= d.createElement('style');
            style.textContent = "...";
            if(1) {
                style.textContent = style.textContent.replace('-inline-start', '-left').replace('-inline-end', '-right');
            }
        /**/} else {
            style = d.createElement('link');
            style.setAttribute('rel', 'stylesheet');
            style.setAttribute('href', cs||'MdbR/settings.css');
            if(location.href.startsWith('vscode')) {
                style.setAttribute('href', 'http://192.168.0.100:8080/base/3/MdbR/settings.css');
            }
        }/**/
        var debug = function(a,b,c,d,e){var t=[a,b,c,d,e];for(var i=5;i>=0;i--){if(t[i]===undefined)t[i]='';else break}w.console.log("%c SETTINGS ","color:#333!important;background:#0FF;",t[0],t[1],t[2],t[3],t[4])};
        function ge(e,p){return (p||d).getElementById(e)};
        function gc(e,p){return (p||d).getElementsByClassName(e)[0]};
        function gt(e,p){return (p||d).getElementsByTagName(e)[0]};
        function craft(c, p, t) {
            var e=d.createElement(t||'DIV');
            if(c)e.className=c;
            if(p)p.appendChild(e);
            return e;
        }
        function getObj(e, c) {
            var p=e.path;
            if(!p && e.composedPath) p=e.composedPath();
            if(p) {
                for(var i=0;t=p[i++];)
                    if(t.classList.contains(c))
                        return t;
            } else {
                var cc=10,t=e.srcElement;
                while(cc-->0 && t) {
                    //debug(t.classList);
                    if(t.classList.contains(c))
                        return t;
                    t = t.parentNode;
                }
            }
        }
        
        // json side : 
        // settings array : [(presenter type)|(prefix type)<<10, id, title, value]
        // 	id array [id, id1, val1 if selected, is selected]  id1, val1 与prefix有关（复选框/单选框）
        // 	title array [title, subtitle]
        
        // dom side : 
        // settings row : [Prefix] Title[SubTitle] [Presenter]
        // [prefix type]  0无。1单选框。2复选框。 3树形可展开标志。
        // [presenter type] 0 none/1 toggle/2 input/3 select/4 slider
        function clickToggle(e) {
            //debug(e, e.composedPath());
            var t=getObj(e, 'outerRow');
            var toggle=gc('toggle',t);
            debug(t.id);
            var v=!toggle.classList.contains('enabled');
            var pv;//=t.pf(t.id, v, t, false, t.e[3]);
            if(t.sel!=undefined) {
                var val=null;
                if(v) {
                    var p=t.childList,c=t.childList,defirst;
                    if(c) {
                        c = c.firstChild;
                        while(c) {
                            //debug('same group?', p, c, c.id1, t.id1, c.id1===t.id1);
                            //if(c.id1===t.id1)debug('prefix?', c, c.id1, t.id1, gc('prefix',c));
                            if(c.id1===t.id1) {
                                if(!defirst)
                                    defirst = c;
                                if(gc('prefix',c)/*?*/.classList.contains('enabled')) {
                                    val = c.sel;
                                    break;
                                }
                            }
                            c=c.nextSibling;
                        }
                    }
                    if(val==null && defirst) {
                        val = defirst.sel;
                        gc('prefix',defirst).classList.add('enabled');
                        if(defirst.childList) defirst.childList.style.display = 'block';
                    }
                } else {
                    val=t.sel;
                }
                pv=t.pf(t.id1, val);
            } else {
                pv=t.pf(t.id, v);
            }
            if(pv){
                if(v) toggle.classList.add('enabled');
                else toggle.classList.remove('enabled');
                if(t.childList) {
                    t.childList.style.display=v?'block':'none';
                }
            }
            e.stopPropagation();
        }
        function clickPrefix(e) {
            e.stopPropagation();
            var t=getObj(e, 'outerRow');
            var p=gc('prefix', t);
            debug('clickPrefix', t.id, p, t.childList);
            var ps=p.classList;
            var is_radio = ps.contains('radio');
            var option =  is_radio && (t.sel!=undefined);
            var v=!ps.contains('enabled');
            var pv;//=t.pf(is_radio?t.id1:t.sel, option?t.sel:v, t, false, t.e[1][2]);
            if(t.id1==='') {
                pv=1;
            } else if(t.sel!=undefined) {
                var val=v?t.sel:false;
                if(option) {
                    val = t.sel;
                }
                else if(!is_radio && t.childList) {
                    //...
                }
                pv=t.pf(t.id1, val);
            } else {
                pv=t.pf(t.id1, v);
            }
            if(pv){
                if(ps.contains('checkbox') || ps.contains('fold')) {
                    if(v) ps.add('enabled');
                    else ps.remove('enabled');
                    if(t.childList) {
                        t.childList.style.display=v?'block':'none';
                    }
                }
                else if(ps.contains('radio')) {
                    if(pv===true) { // radio checkbox
                        if(v) ps.add('enabled');
                        else ps.remove('enabled');
                        if(t.childList) {
                            t.childList.style.display=v?'block':'none';
                        }
                    }
                    if(pv===1) { // radio option
                        ps.add('enabled');
                        if(1) { // t.id1
                            var p=t.parentNode,c=p.firstChild;
                            while(c) {
                                //debug('same group?', p, c, c.id1, t.id1, c.id1===t.id1);
                                if(c.id1===t.id1) {
                                    //debug('same group=', c);
                                    if(c!=t) {
                                        if(c.childList) {
                                            c.childList.style.display='none';
                                        }
                                        var cp = gc('prefix',c);
                                        if(cp) cp.classList.remove('enabled');
                                    }
                                }
                                c=c.nextSibling;
                            }
                        }
                        if(t.childList) {
                            t.childList.style.display='block';
                        }
                    }
                }
            }
        }
        var changedTextIds=[];
        function changeText(ev) {
            var t=getObj(ev, 'outerRow'), id=t.id, tid=changedTextIds[id];
            if(tid) clearTimeout(tid.id);
            changedTextIds[id] = {id:setTimeout(commitText, 600), t:t};
            // debug(ev);
        }
        function commitText(e) {
            for (var k in changedTextIds) {
                var tid=changedTextIds[k],t=tid.t,input=t.input;
                var pv = t.pf(t.id, input.value, t);

            }
            changedTextIds.length = 0;
        }

        function buildCard (pref, array, host, card, parent) {
            if(!host.shadow) {
                if(host.attachShadow&&1) {
                    host.shadow = host.attachShadow({mode: 'open'});
                } else {
                    // 安卓4不支持 shadow dom，直接运用普通dom
                    host.shadow = craft(0,host);
                }
                host.shadow.appendChild(style.parentNode?style.cloneNode():style);
            }
            if(!host.cards) {
                host.cards = [];
            }
            host.pref = pref;
            if(!card) {
                card = craft('card', host.shadow);
                host.cards.push(card);
            }
            if(!host.root) {
                host.root=card;
            }
            if(!parent) {
                parent = card;
            }
            //card.a = array;
            for (var i=0;i<array.length;i++) {
                if(i>0)craft('hr',parent);
                var e=array[i];
                if (Array.isArray(e)) {
                    var mask=Math.pow(2,10)-1;
                    var prpr = e[0];
                    var presenter=prpr&mask;
                    var prefix=prpr>>10;
                    var id = e[1], id1=null, sel=null, val1=null;
                    if(Array.isArray(id)) {
                        if(id.length==2) {
                            debug('id.length==2', id, val1, presenter==0, e[3]);
                            sel=id[1];
                            id1=id=id[0];
                        }
                        else if(id.length==3) {
                            if(typeof id[1]=='string') {
                                id1=id[1];
                                val1=id[2];
                                id=id[0];
                                sel=null;
                            } else {
                                sel=id[1];
                                val1=id[2];
                                id1=id=id[0];
                            }
                        }
                        else if(id.length==4) {
                            id1=id[1];
                            sel=id[2];
                            val1=id[3];
                            id=id[0];
                        }
                    }
                    var label = e[2];
                    var value = e[3];
                    if(presenter==0 && val1==null) {
                        val1 = value;
                    }
                    var outerRow = craft('outerRow'+(parent!=card?' list-item':'')+(presenter<2?' hot':''),parent);
                    var outerClick=true;
                    outerRow.id=id;
                    outerRow.pf=pref;
                    outerRow.sel=sel;
                    outerRow.id1=id1;
                    outerRow.e=e;

                    if(prefix==1) {
                        debug('radio prefix', id, value, val1);
                        var wrapper = craft('radio prefix'+(val1?' enabled':''),outerRow);
                        var diskBorder = craft('disc-border',wrapper);
                        var disk = craft('disc',wrapper);
                        wrapper.onclick=clickPrefix;
                        outerClick=false;
                    }
                    if(prefix==2) {
                        var wrapper = craft('checkbox prefix'+(val1?' enabled':''),outerRow);
                        var checkmark = craft('checkmark',wrapper);
                        wrapper.onclick=clickPrefix;
                        outerClick=false;
                    }
                    if(prefix==3) {
                        var wrapper = craft('fold prefix'+(val1?' enabled':''),outerRow);
                        var foldmark = craft('settings-close settings-fold',wrapper);
                        wrapper.onclick=clickPrefix;
                        outerClick=false;
                    }
                    var labelWrapper = craft('flex labelWrapper'+(prefix?' second':''),outerRow);
                    if (Array.isArray(label) && label.length>1) {
                        var prilabel = craft('label',labelWrapper);
                        var secLabel = craft('secondary label',labelWrapper);
                        prilabel.innerText=label[0];
                        secLabel.innerText=label[1];
                    } else {
                        labelWrapper.innerText=label;
                    }
                    if(prefix) {
                        (presenter?labelWrapper:outerRow).onclick=clickPrefix;
                    }
                    if(presenter==1) {
                        var toggle = craft('toggle'+(value?' enabled':''),outerRow);
                        var tBar = craft('bar',toggle);
                        var tKnob = craft('knob',toggle);
                        outerRow.tKnob=tKnob;
                        if(outerClick)
                            outerRow.onclick=clickToggle;
                        toggle.onclick=clickToggle;
                    }
                    if(presenter==2) {
                        //  prpr, id, title[], value, hint
                        var wrapper = craft('flex labelWrapper',outerRow);
                        var input = outerRow.input = craft(0,wrapper,'INPUT');
                        input.value=value;
                        input.placeholder=e[4]||value;
                        input.oninput=changeText;
                    }
                    var subst=e.length-1;
                    while(Array.isArray(e[subst])) {
                        subst--;
                    }
                    if(subst<e.length-1) {
                        var list=craft('list-frame', parent);
                        list.style.display=value?'block':'none';
                        outerRow.childList = list;
                        buildCard(pref, e.slice(subst+1), host, parent, list);
                    }
                }
            } 
            return card;
        }
        function tweakDialog(host, width, showClose) {
            var pw=host.parentNode.offsetWidth;
            if(width===undefined) {
                var w='100%';
                if(pw<480) 
                {
                    if(showClose===undefined) {
                        showClose = true;
                    }
                } else {
                    if(pw<700) {
                        w = '80%';
                    } else {
                        w = '70%';
                    }
                    if(!host.mask) {
                        var mask = craft('settings-mask', 0);
                        mask.style='height:100%;width:100%;top:0px;position:fixed;';
                        mask.onclick = host.dismiss;
                        mask.oncontextmenu = function(e) {
                            e.preventDefault();
                            var sty=host.style;
                            if(e.pageX<d.documentElement.clientWidth/2) {
                                sty.right=sty.left==='unset'?0:'unset';
                                sty.left=0;
                            } else {
                                sty.left=sty.right==='unset'?0:'unset';
                                sty.right=0;
                            }
                            debug('right1', host, host.style.right==='unset', host.style.right);
                        };
                        host.parentNode.prepend(mask);
                    }
                }
                host.style.width = w;
            }
            if(true) {
                var dlg = host.dlg;
                if(!dlg.listeners) {
                    var dragState = {
                        x : 0,
                        y : 0,
                        drag : false,
                        back : false
                    };
                    dlg.listeners = [function(e){ // down
                        if(dragState.drag) {
                            dragState.drag = false;
                        }
                        dlg._drag=(e.touches?e.touches[0].clientX:e.clientX) < dlg.offsetWidth/10*3;
                    }, function(e){ //move
                        if(dlg._drag) {
                            var lastX, lastY;
                            if(e.changedTouches) {
                                lastX=e.changedTouches[0].clientX;lastY=e.changedTouches[0].clientY;
                            } else if(e.touches) {
                                lastX=e.touches[0].clientX;lastY=e.touches[0].clientY;
                            } else {
                                lastX=e.clientX;lastY=e.clientY;
                            }
                            var tempX = lastX;
                            var tempY = lastY;
                            var diff_x = tempX - dragState.x;
                            var diff_y = Math.abs(tempY - dragState.y);
                            var startSlope=5;
                            if(!dlg._dragging) {
                                if(!dragState.drag) {
                                    dragState.x = lastX;
                                    dragState.y = lastY;
                                    dragState.drag = true;
                                }
                                if(diff_x > startSlope && diff_y < startSlope*1.25) {
                                    var discBack = dlg.host.discBack;
                                    if(!discBack) {
                                        discBack = craft('settings-close-disc', dlg.host.shadow);
                                        var arrow = craft('settings-back-arrow', discBack);
                                        discBack.style = 'width:48px;height:48px;display:flex;align-items:center;justify-content:center;left:13px;top:0;bottom:0;right:auto;margin:auto;';
                                        arrow.style = 'transform:rotate(180deg);margin-top:2px;color:#1a73e8;font-size:19px;';
                                        arrow.innerText = '➤';
                                        dlg.host.discBack = discBack;
                                    }
                                    discBack.style.display = 'flex';
                                    dlg._dragging=1;
                                } else if(diff_y >= Math.max(17.5*diff_x, 14.5*startSlope)) {
                                    // dlg._drag = false;
                                    // touch_p.drag = false;
                                }
                            }
                            if(dlg._dragging) {
                                // 修改位移与透明度
                                var sty=dlg.host.discBack.style;
                                var theta=50;
                                var dx=diff_x-startSlope;
                                dragState.back = dx>=theta;
                                sty.left = Math.min(dx, theta)+'px';
                                sty.opacity = Math.min(dx/theta, 1);
                                e.preventDefault();
                                e.stopPropagation();
                            }
                        }
                    }, function(){ // up
                        if(dlg._drag) {
                            if(dlg._dragging) {
                                dlg.host.discBack.style.display = 'none';
                            }
                            if(dragState.back) {
                                dragState.back = false;
                                hideDialog(dlg);
                            }
                            dlg._drag=false;
                            dlg._dragging=false;
                            //debug('Touch end('+dragState.c_x+', '+dragState.c_y+')');
                        }
                    }, function(e){ // click
                        if(dragState.drag) {
                            e.preventDefault();
                            e.stopPropagation();
                            dragState.drag = false;
                        }
                    }];
                    var ae=dlg.addEventListener;
                    ae('touchstart',dlg.listeners[0]);
                    ae('mousedown',dlg.listeners[0]);
                    ae('click',dlg.listeners[3], true);
        
                    ae('touchmove',dlg.listeners[1]);
                    ae('mousemove',dlg.listeners[1]);
                    ae('touchend',dlg.listeners[2]);
                    ae('mouseup',dlg.listeners[2]);
                }
            }
            //debug('tweakSettingsDialog', host.parentNode, pw, pw<480, host.style.width);
            var disc = host.close;
            if(showClose) 
            {
                if(!disc) {
                    disc = craft('settings-close-disc', host.shadow);
                    host.close = disc;
                    disc.onclick = host.dismiss;
                    craft('settings-close', disc);
                }
                disc.style.display='block';
            } else if(disc){
                disc.style.display='none';
            }

            host.root.style.width = width;
        }
        function getDialogHolder(id) {
            var dlg=ge(id);
            if(!dlg) {
                dlg = craft(0, d.body);
                dlg.id = id;
                dlg.style = 'width:100%;height:100%;top:0;position:fixed;transition:transform 200ms,opacity 200ms;background:#8080800d;transform:scale(0.7);-webkit-transform:scale(0.7);opacity:0;z-index:999;'

                var host = craft('settings-host', dlg);
                host.style = 'width:70%;height:100%;margin:auto;right:0;left:0;overflow-y:auto;position:fixed;';
                host.dismiss = function(e) {
                    hideDialog(dlg);
                }
                dlg.host=host;
                host.dlg=dlg;
            }
            return dlg;
        }
        function hideShow(dlg, show) {
            dlg = dlg.style; 
            if(show) {
                dlg.transform = 'scale(1)';
                dlg.webkitTransform = 'scale(1)';
                dlg.opacity = '1';
            } else {
                dlg.transform = 'scale(0.7)';
                dlg.webkitTransform = 'scale(0.7)';
                dlg.opacity = '0';
            }
        }
        function showDialog(dlg) {
            setTimeout(function(){SettingsTweakDialog(dlg.host);hideShow(dlg, true);}, 16);
            dlg.style.display='block';
        }

        function hideDialog(dlg) {
            hideShow(dlg, false);
            setTimeout(function(){dlg.style.display='none'}, 200);
            if(dlg.host.pref) {
                dlg.host.pref('close');
            }
        }
        w.SettingsBuildCard = buildCard;
        w.SettingsTweakDialog = tweakDialog;
        w.SettingsGetDialogHolder = getDialogHolder;
        w.SettingsShowDialog = showDialog;
        w.SettingsHideDialog = hideDialog;
    })();
}