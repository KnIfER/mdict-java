/* License GPL3 + 四绝协议 modify notes in db in random order. */ 
(function(){
    var doc = document;
    var log = function(a,b,c,d,e){var t=[a,b,c,d,e];for(var i=5;i>=0;i--){if(t[i]===undefined)t[i]='';else break}console.log("%c ANNOT ","color:#000;background:#ffaaaa;",t[0],t[1],t[2],t[3],t[4])}
	function getNextNode(n, e) {
        var a = n.firstChild;
        if (a) {
            if(!skipIfNonTex(n)) { // 将这些考虑为没有文本的节点；不要进去。
                return a;
            }
            if(a==e) {
                return null;
            }
        }
		while (n) {
			if (a = n.nextSibling) {
				return a
			}
			n = n.parentNode
		}
	}
	function getNextNodeRaw(n) {
        var a = n.firstChild;
        if (a) return a;
		while (n) {
			if (a = n.nextSibling) {
				return a
			}
			n = n.parentNode
		}
	}
	function getNodesInRange(r) {
        //r=getSelection().getRangeAt(0);
		var b = [];
		var s = r.startContainer;
		var e = r.endContainer;
		var a = r.commonAncestorContainer;
		var n;
		for (n = s.parentNode; n; n = n.parentNode) {
			b.push(n);
			if (n == a) {
				break
			}
		}
		b.reverse();
		for (n = s; n; n = getNextNode(n, e)) {
            if(!n) break
			b.push(n);
			if (n == e) {
				break
			}
		}
		return b
	}
	function getNodeIndex(n) {
		var a = 0;
		while ((n = n.previousSibling)) {
			++a
		}
		return a
	}
	function insertAfter(b, n) {
		var a = n.nextSibling,
			c = n.parentNode;
		if (a) {
				c.insertBefore(b, a)
			} else {
				c.appendChild(b)
			}
		return b
	}
	function splitDataNode(n, a) {
		var b = n.cloneNode(false);
		b.deleteData(0, a);
		n.deleteData(a, n.length - a);
		insertAfter(b, n);
		return b
	}
	function isCharacterDataNode(b) {
		var a = b.nodeType;
		return a == 3 || a == 4 || a == 8
	}
	function splitRangeBoundaries(r) {
		var s = r.startContainer,
			o = r.startOffset,
			e = r.endContainer,
			l = r.endOffset;
		var d = (s === e);
		if (isCharacterDataNode(e) && l > 0 && l < e.length) {
				splitDataNode(e, l)
			}
		if (isCharacterDataNode(s) && o > 0 && o < s.length) {
				s = splitDataNode(s, o);
				if (d) {
					l -= o;
					e = s
				} else {
					if (e == s.parentNode && l >= getNodeIndex(s)) {
						++l
					}
				}
				o = 0
			}
		r.setStart(s, o);
		r.setEnd(e, l)
	}
	function getTextNodesInRange(b) {
		var f = [];
		var a = getNodesInRange(b);
		for (var c = 0, e, d; e = a[c++];) {
			if (e.nodeType == 3) {
				f.push(e);
			}
		}
		return f;
	}

    // attach back link to a refrence row
    function MultiRefLnk(doc, rootNode, backNid, refNid, refRow) {
        if(refRow) {
            function craft(t, p, c) {
                t = doc.createElement(t);
                t.nid = backNid;
                if(c)t.className=c;
                if(p)p.appendChild(t);
                return t;
            }
            if(!refRow.refSelf) {
                refRow.refSelf = true;
                MultiRefLnk(doc, rootNode, refRow.nid, refNid, refRow);
                var b = rootNode._pd_bn;
                if(b) b=b[refNid];
                if(b) {
                    for(var i;i<b.length;i++) {
                        MultiRefLnk(doc, rootNode, b[i], refNid, refRow);
                    }
                }
            }
            var sup = craft('SUP',0, 'RefSup');
            var lnk = craft('A', sup, 'RefBacks');
            lnk.href = '_pd_sup'+backNid;
            refRow.insertBefore(sup, refRow.lastElementChild)
            lnk.addEventListener('click', doc._pd_clks[1], 1);
        } else {
            var b = rootNode._pd_bn;
            if(!b) b=rootNode._pd_bn={}
            if(!b[refNid]) b[refNid]=[]
            b[refNid].push(backNid);
        }
    }

	function wrapRange(r, el, rootNode, doc, nid, neo) {
        var tcn = el.tcn;
        if(!el){
            el = doc.createElement("ANNOT");
            el.style="background:#ffaaaa;";
        }
        if(!r) r = getSelection().getRangeAt(0);
        //log('wrapping...',r.startContainer, r.startOffset, r.endContainer, r.endOffset);
		splitRangeBoundaries(r);
        //log('splitRangeBoundaries...',r.startContainer, r.startOffset, r.endContainer, r.endOffset);
        //getSelection().empty(0); getSelection().addRange(r);
        if(!doc) doc = document;
        if(!rootNode) rootNode = doc.body;
		var f = getTextNodesInRange(r);
		if (f.length == 0) {
			return;
		}
        //var nodes = [];
        var first=0, last;
		for (var c = 0, e, d; e = f[c++];) {
			if (e.nodeType == 3) {
                //log(e);
				d = el.cloneNode(false);
				d.bg = tcn.bg;
                if(!first) {
                    d.id = '_pd_annot'+nid;
                    first = d;
                }
                last = d;
                // if(e.parentNode.tagName==='ANNOT' && e.parentNode.bg==d.bg) {
                //     whiterRgb(d, e.parentNode)
                // }
				e.parentNode.insertBefore(d, e);
				d.appendChild(e);
                //nodes.push(d);
                d.nid = nid;
			}
		}
        first.end = last;
		r.setStart(f[0], 0);
		var a = f[f.length - 1];
		r.setEnd(a, a.length);
        ///log('fatal web annot::wrapRange::', r);
        if(tcn.note) { // 文本笔记
            var noteType=tcn.ntyp, note=tcn.note;
            //noteType=1;
            d = el.cloneNode(false);
            d.className = 'note';
            d.id = '_pd_note'+nid;
            d.nid = nid;
            insertAfter(d, last);
            function craft(t, p, c) {
                var e=doc.createElement(t);
                e.nid = nid;
                if(c)e.className=c;
                (p||d).appendChild(e);
                return e;
            }
            function bubbleInto(el) {
                var id = '_pd_bsty'
                var sty = doc.getElementById(id);
                if(!sty) {
                    sty = craft('STYLE', doc.head);
                    sty.innerText = '._PDBP{position:relative}._PDBV::after{content:attr(data-tex)}._PDBX{position:absolute;left:95%;bottom:80%;}._PDB{border-radius: 16px;background:#abcdef;color:#fff;white-space:nowrap;padding:0px 8px;margin:0px;font-size:14px;}._PDBF>A{color:white}._PDBF{padding:2px 8px}';
                    sty.id = id;
                    sty.click = function(){
                        log('点击！！！');
                    }
                }
                if(el) {
                    el.style.position = 'relative';
                    el = craft('Annot',el,'_PDB _PDBX note');
                    // if(tcn.bnt) // no text selection
                    //     el.classList.add('_PDB_NT');
                    // if(tcn.bnc) // no click
                    //     el.classList.add('_PDB_NC');
                    //else el // click to edit note
                    el.addEventListener('click', sty.click);
                    if(tcn.bclr) 
                        el.style.background = toRgb(tcn.bclr);
                    return el;
                }
            }
            if(noteType==0) { // 正文
                if(note[0]!='(' && note[1]!='(')
                    note = ' ('+note+') ';
                d.innerText = note;
                if(tcn.bin) {
                    var bel=bubbleInto(d);
                    bel.innerHTML = '&emsp;';
                }
                if(tcn.fclr) 
                    d.style.color = toRgb(tcn.fclr);
                if(tcn.fsz) 
                    d.style.fontSize = (parseInt(tcn.fsz)||100)+'%';
                last = d;
            }
            else if(noteType==1) { // 气泡
                var bel = bubbleInto(last);
                if(tcn.bon) {
                    bel.innerText = note;
                } else {
                    //bel.innerText = '···';
                    bel.classList.add('_PDBV');
                    bel.setAttribute('data-tex', '···');
                }
				bel.onclick = function(e){
					debug('bid='+nid, e, e.target);
					app.editNode(sid.get(), e.target.nid);
				}
                if(tcn.fclr) 
                    bel.style.color = toRgb(tcn.fclr);
                if(tcn.fsz) 
                    bel.style.fontSize = (parseInt(tcn.fsz)||100)+'%';
                d.parentNode.removeChild(d);
                last = bel;
            }
            else { // 脚注
                //log('footnote=', note);
                last = d;
                var lnkTo = null;
                if(note.startsWith('_pd_lnk='))
                    lnkTo = parseInt(note.slice(8));
                var id = '_pd_ftsy'
                var sty = doc.getElementById(id);
                if(!sty) {
                    sty = craft('STYLE', doc.head);
                    sty.innerText = '.RefList{padding-left:10px}.RefList>LI:focus{background:rgba(5,109,232,.08)}SUP:focus{box-shadow:0 0 0 2px #ffffff, 0 0 0 4px rgb(5 109 232 / 30%)}.RefBack{color:#175199;padding-right:0.25em;font-weight:600;}.RefBack,SUP>A{text-decoration:none}.RefList>LI{counter-reset:_pd_ref}.RefSup{color:#175199;padding-right:0.25em;font-weight:600}.RefBacks:before{counter-increment:_pd_ref;content:counter(_pd_ref, lower-alpha)}._pdn_sup:before{counter-increment:_pdn_sup;content:\'[\'counter(_pdn_sup)\']\'}._PDict_body,body{counter-reset:_pdn_sup}';
                    sty.id = id;
                }
                var clk = doc._pd_clks;
                if(!clk)
                clk = doc._pd_clks = [function(e) {
                    var t = e.target || e.srcElement; 
                    e.preventDefault(); e.stopPropagation();
                    log('  focus', t);
                    t = t.href+'';
                    t = doc._pd_foc = doc.getElementById(t.slice(t.indexOf('#')+1));
                    if(window.PDF) {
                        var p=t;while((p=p.parentNode)) {if(p.classList.contains('RefListP')){onShowRefList(p, t);break}}
                    } else {
						t.focus();
						if(window._combo) {
							app.setScrollY(sid.get(), t.getBoundingClientRect().top);
						}
					}
                    return true;
                }, function(e) {
                    var t = e.target || e.srcElement; 
                    e.preventDefault(); e.stopPropagation();
					t = doc.getElementById('_pd_sup'+t.nid);
                    t.focus();
					if(window._combo) {
						app.setScrollY(sid.get(), t.getBoundingClientRect().top);
					}
                    return true;
                }, function(e) {
                    var t = e.target || e.srcElement; 
                    if(doc._pd_foc===t) {
						var tmp = doc.getElementById('_pd_sup'+t.nid);
                        tmp.focus();
						if(window._combo) {
							app.setScrollY(sid.get(), tmp.getBoundingClientRect().top);
						}
                    }
                    doc._pd_foc = t;
                }]

                if(lnkTo!==null)
                    id = '_pd_ref'+lnkTo;
                else
                    id = '_pd_ref'+nid;
                var has=doc.getElementById(id);
                var num = 0;
                //if(!neo) {
                if(0) {
                    num = rootNode._pd_refs||0;
                    if(!has) {
                        num = rootNode._pd_refs = num+1;
                    }
                } else {
                    if(rootNode._pd_ref) {
                        function reduce(arr, p, st, ed) { // 二分法
                            var len = ed - st;
                            if (len > 1) {
                              len = len >> 1;
                              //log('reduce', st, len, ed);
                              return p > (arr[st + len - 1].tPos||0)
                                        ? reduce(arr, p, st+len, ed)
                                        : reduce(arr, p, st, st+len);
                            } else {
                              return st;
                            }
                        }
                        var lst = rootNode._pd_ref.childNodes;
                        num = (reduce(lst, tcn.tPos, 0, lst.length))||0;
                        log('reduce=', tcn.note, num)
                    }
                }
                var sup = craft('SUP');
                var lnk = craft('A', sup);
                //lnk.innerText = '['+num+']';
                lnk.className = '_pdn_sup';
                lnk.href = '#'+id;
                sup.id = '_pd_sup'+nid;
                lnk.addEventListener('click', clk[0], 1);
                if(tcn.bin) {
                    bubbleInto(0);
                    sup.classList.add('_PDB')
                    sup.classList.add('_PDBF')
                }
                sup.setAttribute('tabindex',0);

                if(!rootNode._pd_ref) {
                    var refP = craft('ANNOT', rootNode, 'note RefListP');
                    var head = craft('H2', refP);
                    head.innerText = '笔记';
                    if(window.PDF) {
                        head = craft('P', head, 'RefClose RefBack');
                        head.innerText = '[X]';
						refP.cbtn = head;
                        head.onclick=function() {
							onHideRefList(refP);
                        }
                    }
                    rootNode._pd_ref = craft('OL', refP, 'RefList');
                }
                if(lnkTo!==null) {
                    MultiRefLnk(doc, rootNode, nid, lnkTo, has);
                }
                else if(!has) {
                    var row = craft('LI', rootNode._pd_ref);
                    row.setAttribute('tabindex',0)
                    row.id = id;
                    row.tPos = tcn.tPos;
                    lnk = craft('A', row, 'RefBack');
                    lnk.innerText = '^';
                    lnk.href = '#'+sup.id;
                    craft('SPAN', row).innerText = note;
                    row.addEventListener('click', clk[2]);
                    lnk.addEventListener('click', clk[1], 1);
                    if(true && num>=0) {
                        var nodes = rootNode._pd_ref.childNodes, n = nodes[num], ibf=0;
                        while(n && n.tPos>tcn.tPos) {
                            ibf = n;
                            n = n.nextElementSibling;
                        }
                        if(ibf && ibf!=row) rootNode._pd_ref.insertBefore(row, ibf);
                    }
                }
            }
            log('笔记=', d, last)
        }
        first.end = last;
        //log('last=',tcn.note,first,last);
	}
    function getNodeIndex(node) {
        var i = 0;
        while( (node = node.previousElementSibling) ) {
            //log(node);
            ++i;
        }
        return i;
    }
    
	// function getNextNode(n) {
	// 	var a = n.firstChild;
	// 	if (a) {
	// 		return a
	// 	}
	// 	while (n) {
	// 		if ((a = n.nextSibling)) {
	// 			return a
	// 		}
	// 		n = n.parentNode
	// 	}
	// }
	function getRelIndex(n) {
		var a = 0;
		while ((n = n.previousElementSibling)) {
            if(!skip(n))
			    ++a
		}
		return a
	}
    // serialize node, offset as path:offset
    function storePos(n, o, rootNode) {
        var ret = [], p = n.parentNode;
        while (p && skip(p)) {
            p = p.parentNode;
        }
        // 获得原始非标注父节点
        if(p) {
            for (var t=p; t; t = getNextNode(t)) {
                if (t == n) {
                    break
                }
                if(t.nodeType == 3) {
                    o += t.length;
                }
            }
            while (p && p != rootNode) {
                ret.push(getRelIndex(p, true));
                p = p.parentNode;
            }
        }
        ret = ret.reverse();
        return ret.join("/") + ":" + o;
    }
    function is_all_ws(nod) {
        return !(/[^\t\n\r ]/.test(nod.textContent));
    }
    // extract the semi-real text position
    function storeTextPos(n, o, rootNode) {
        var p = n.parentNode;
        while (p && skip(p)) {
            p = p.parentNode;
        }
        // 获得原始非标注父节点
        var debug = false;
        if(p) {
            for (var t=p; t; t = getNextNode(t)) {
                if (t == n) {
                    break
                }
                if(t.nodeType == 3) {
                    if(debug) log('1::', t, t.parentNode, t.nodeType, t.length);
                    o += t.length;
                }
            }
            if(debug) log('');
            for (var t=rootNode; t; t = getNextNode(t)) {
                //if(debug)  log('2::', t);
                if (t == p) {
                    break
                }
                if(t.nodeType == 3 && !is_all_ws(t)) {
                    o += t.length;
                    if(debug)  log('2::len=', t.length, o, t, "tex="+t.nodeValue);
                }
            }
        }
        //log('storeTextPos', o);
        return o;
    }
    function skip(n) {
        if(n.nodeType==1)
            return n.tagName=='ANNOT'||n.tagName=='STYLE'||n.tagName=='LINK'||n.tagName=='SCRIPT'||n.tagName=='MARK'||n.classList.contains('_PDict')
            ||n.tagName=='IFRAME' || n.classList.contains('skiptranslate') && (n.firstElementChild||n).classList.contains('goog-te-banner-frame');
        else return n.nodeType!==3;
    }
    function skipIfNonTex(n) {
        if(n.nodeType==1)
            return (n.tagName=='ANNOT'&&n.classList.contains('note'))||n.tagName=='STYLE'||n.tagName=='LINK'||n.tagName=='SCRIPT'||n.classList.contains('_PDict')
            ||n.tagName=='IFRAME' || n.classList.contains('skiptranslate') && (n.firstElementChild||n).classList.contains('goog-te-banner-frame');
        else return n.nodeType!==3;
    }
    // deserialize saved position str as [node, offset]
    function restorePos(str, rootNode, ex) {
        var parts = str.split(":");
        var node = rootNode;
        var nodeIndices = parts[0] ? parts[0].split("/") : [], i = 0, ln=nodeIndices.length, nodeIndex;

        while (i<ln) {
            nodeIndex = parseInt(nodeIndices[i], 10);
            var n = node.firstElementChild;
            while (n && skip(n)) {
                n = n.nextElementSibling;
            }
            while(nodeIndex>0 && n) { //???
                n = n.nextElementSibling;
                if(n && !skip(n))
                    nodeIndex--;
            }
            if (!n) {
                log("'"+str.replaceAll('/',' ')+"'", " has no child with indice ",nodeIndices[i], + (i+1)+'/'+ln+"/"+node.childNodes.length+" at::", node);
                node = 0;
                break;
            }
            //log(str, " found " + nodeIndices[i] + ", " + (i)+'/'+ln + ", " + node.childNodes.length, node, n);
            node = n;
            i++;
        }
        if (!node) {
            return 0;
        }
        var o=0,l=parseInt(parts[1], 10),ln=l;
        if(ex) ln++;
        //log("restorePos at : ", node, l);
        for (var t=node; t; t = getNextNode(t)) {
            //if (t == ed) break; //未作边界检查，实际t可能超出初始node范围
            if(t.nodeType == 3) {
                //log("restorePos : ", t, t.length, o);
                if(o + t.length>=ln) { // ???
                    node = t;
                    l -= o;
                    break;
                }
                o += t.length;
            }
        }

        return [node, l]; // node, offset
    }

    function test(r) {
        // r = getSelection().getRangeAt(0);
        // var rootNode = doc.body;
        // return storePos(r.startContainer, r.startOffset, rootNode);
        range = storeRange()
        getSelection().empty();
        getSelection().addRange(makeRange(range[0], range[1]))
    }

    
    function restoreRange() {
        getSelection().empty();
        getSelection().addRange(makeRange(range[0], range[1]))
    }

    function getdocRangy(node) {
        if (node.nodeType == 9) {
            return node;
        } else if (typeof node.ownerdoc != undefined) {
            return node.ownerdoc;
        } else if (typeof node.doc != undefined) {
            return node.doc;
        } else if (node.parentNode) {
            return getdocRangy(node.parentNode);
        } else {
            throw module.createError("getdoc: no doc found for node");
        }
    }

    var range;
    function storeRange(r) {
        r = getSelection().getRangeAt(0);
        //var rootNode = this.getdocRangy(range.startContainer).docElement;
        var rootNode = doc.body;
        return [storePos(r.startContainer, r.startOffset, rootNode), storePos(r.endContainer, r.endOffset, rootNode)];
    }
    function store(r, rootNode) {
        var p1 = storePos(r.startContainer, r.startOffset, rootNode);
        var p2 = storePos(r.endContainer, r.endOffset, rootNode);
        var b = p1.length > p2.length, ln = b?p1.length:p2.length, i=0;
        for(;i<ln;i++) {
            if(p1[i]!=p2[i]) break;
        }
        if(i>0) return p1.substring(0, i)+';'+p1.substring(i)+';'+p2.substring(i);
        else return p1+';'+p2;
    }
    function restore() {
        getSelection().empty();
        getSelection().addRange(makeRange(range[0], range[1]))
    }
    
    function makeRange(r0, r1, rootNode, d, tcn) {
        doc = d?d:document;
        if(!rootNode) rootNode = doc.body;
        //if(app.done) return; app.done=1;
        if(r0.length==0||r1.length==0) return 0;
        //var doc = doc.body;
        //var result = serialized.split(',');
        //todo checksum
        //log('1__'+result[0]);
        //log('2__'+result[1]);
        var start = restorePos(r0, rootNode, 1), end = restorePos(r1, rootNode);
        var range = new Range();
        if(start&&end) {
            try{
                range.setStart(start[0], start[1]);
                range.setEnd(end[0], end[1]);
				//tcn.check = 0;
                if(tcn && tcn.check) {
                    var k=tcn.check, k1 = '', s1=tcn.d?2:4, b1=k.length==s1;
                    try{
                        k1 = range.startContainer.data[range.startOffset];
                        if(!tcn.d) k1+=range.endContainer.data[range.endOffset-1];
                    } catch(e) {
                        k1='';
                    }
                    function pass(k1){return k.startsWith(k1)||b1&&k.startsWith(k1,s1)};
                    var p = k1&&pass(k1);
                    if(p) {
                        //log('check range restore 直接验证！ ', k1);
                    } else {
						if(true) { // 修复 <div> 放到 <head> 造成偏移一位的BUG
							range.setStart(start[0], Math.max(0, start[1]-1));
							range.setEnd(end[0], Math.max(0, end[1]-1));
							try{
								k1 = range.startContainer.data[range.startOffset];
								if(!tcn.d) k1+=range.endContainer.data[range.endOffset-1];
							} catch(e) {
								k1='';
							}
							p = k1&&pass(k1);
						}
						if(!p) {
							range.setStart(start[0], start[1]);
							range.setEnd(end[0], end[1]);
							log('check range restore 曲折验证！ ', k1);
							var text = range.toString();
							k1 = text[0];
							if(!tcn.d) k1 += text[text.length-1];
							p = pass(k1);
						}
                    }
                    log('check range restore --- ', p?'pass':'验证失败!', k1+'=='+k, !p&&app.annotRaw(sid.get(), ''+tcn.nid));
                    if(!p) return 0;
                }
                return range;
            } catch(e){}
        }
        return 0;
    }

    function whiterRgb(n, bf, a){
        if(a==null)a=0.25;
        function mw(ch, p){
            return (0x88*p+ch*(1-p));
        }
        var c = bf._bgr;
        if(c===undefined) c=bf.bg;
        var t = (c>>24)&0xff;
        var r = mw((c>>16)&0xff,a);
        var g = mw((c>>8)&0xff,a);
        var b = mw(c&0xff,a);
        var sty = t?"rgba("+r+","+g+","+b+"/"+t+")":"rgb("+r+","+g+","+b+")";
        n.style.backgroundColor = sty;
        log('sty='+sty);
        n._bgr = (t<<24) | (r<<16) | (g<<8) | (b);
    }

    function toRgb(c){
        if(c===undefined) c=0xffffaaaa;
        var t = (c>>24)&0xff;
        var r = (c>>16)&0xff;
        var g = (c>>8)&0xff;
        var b = c&0xff;
        return t && t!=0xff?"rgba("+r+" "+g+" "+b+" / "+parseInt(t*100.0/256)+"%)":"rgb("+r+","+g+","+b+")";
    }

	function getNidsInRange(det) {
        var rg=getSelection().getRangeAt(0);
        log("NidsInRange::", det, rg);
        if(det) {
            var el=rg.startContainer, e=rg.endContainer, p=el.parentNode;
            while(el) {
                if(p && p.tagName==='ANNOT' && p.nid!==undefined) {
                    return 1;
                }
                if(el.tagName==='ANNOT' && el.nid!==undefined) {
                    return 1;
                }
                if(el==e) break;
                el = getNextNodeRaw(el);
            }
            return 0;
        } else {
            var ret='', f = {}, a = getNodesInRange(rg), p = rg.startContainer.parentNode;
            while(p && p.tagName==='ANNOT') {
                a.push(p);
                p = p.parentNode;
            }
            for (var c = 0, e; e = a[c++];) {
                log("\t\tgetNidsInRange::", e, e.nid);
                if (e.nid!=undefined) {
                    if(!f[e.nid]) {
                        f[e.nid]=1;
                        if(ret.length) ret+=',';
                        ret+=e.nid;
                    }
                }
            }
            return ret;
        }
	}

    function deWrap(ds) {
		for (var e = ds.length - 1, d; e >= 0; e--) {
            d = ds[e];
            if(!d.classList.contains('note')) {
                var c = 0;
                for (var f = d.childNodes.length - 1; f >= 0; f--) {
                    var a = d.childNodes[f];
                    d.parentNode.insertBefore(a, c?c:d);
                    c = a
                }
            }
            d.parentNode.removeChild(d)
        }
	}

    function patchNote(nid, tcn) {
        var el = document.getElementById('_pd_annot'+nid);
        log('fatal patchNote::', nid, el, tcn);
        if(el) {
            var e = el.end, nds=[];
            log('patchNote::', nid, el, e);
            while(el) {
                if(el.nid===nid) {
                    nds.push(el);
                    //log('should deWrap::', el);
                }
                if(el==e) break;
                el = getNextNodeRaw(el);
            }
            deWrap(nds);
        }
        el = document.getElementById('_pd_ref'+nid);
        if(el) {
            el.remove();
        }
        if(tcn) {
            var rootNode = document.body, doc = document;
            if(typeof tcn==='string') tcn = JSON.parse(tcn);
            tcn = upackRow(tcn);
            var nn = tcn.n.split(';'), n0=nn[0], n1=nn[1];
            if(nn.length==3) {n0=nn[0]+n1; n1=nn[0]+nn[2]}
            else if(nn.length!=2) return;
            var r = makeRange(n0, n1, rootNode, doc);
            log('fatal log annot::renewing::', tcn, r);
            if(r) {
                var el = annot(tcn, -1, 0, 0);
                wrapRange(r, el, rootNode, doc, nid)
            }
        }
    }

    function annot(tcn, rootNode, doc, pos, bid) {
        log('MakeAnnotation::', tcn);
        if(typeof tcn==='string') tcn = JSON.parse(tcn);
        tcn = upackRow(tcn);
        var type=tcn.typ, color=tcn.clr, note=tcn.note;
        if(type==undefined) type=0;
        if(!doc) doc = document;
        var el = doc.createElement("ANNOT");
        if(type==0) {
            el.className = "PLOD_HL";
            el.setAttribute("style", "background:"+toRgb(color));
        } else {
            el.className = "PLOD_UL";
            //ann.style = "color:#ffaaaa;text-decoration: underline";
            el.setAttribute("style", "border-bottom:2.5px solid "+toRgb(color));
			// el.style.borderBottomStyle='dashed';
			// el.style.textDecoration = "underline 2.5px "+toRgb(color);
        }
        el.tcn = tcn;
        if(rootNode==-1) return el; // -1代表仅创建元素
        var sel = window.getSelection();
        try {
            var nntd = '_pd_nntd'
            var sty = doc.getElementById(nntd);
            if(!sty) {
                sty = doc.createElement('STYLE');
                doc.head.appendChild(sty);
                sty.id = nntd;
            }
            sty.innerText = 'annot.note{visibility:hidden}'; // skip note texts
            var text = sel.toString();
            sty.innerText = '';
            var range = sel.getRangeAt(0);
            var tL=text.length,st=range.startContainer,so=range.startOffset;
            if(tL==0) return;
            if(!rootNode) rootNode = doc.body;
            var tPos = storeTextPos(st, so, rootNode);
            var r = store(range, rootNode);
            tcn.n = r;
            tcn.tPos = tPos;
                var b1 = text.length>1, k1 = '', k2 = text[0];
                tcn.d=0+!b1;
                if(b1) k2+=text[text.length-1];
                try{
                    k1 = st.data[so];
                    if(b1) k1+=range.endContainer.data[range.endOffset-1];
                } catch(e) {
                    k1='';
                    log('装逼失败::',e)
                }
                if(k1==k2) k2='';
            tcn.check = k1 + k2;
            log('tPos='+tPos, 'stored='+r, "check="+tcn.check)
            //app.log('tPos='+tPos+' stored='+r+" check="+tcn.check)
            if(pos==undefined)
                pos = window.currentPos || 0; 
            tcn = packRow(tcn);
            // 保存标记并获得存储id
            var nid = app.annot(sid.get(), text, packTcn(tcn), window.entryKey||null, pos, tPos, type, color, note, bid||null);
            // 创建新标记
            wrapRange(range, el, rootNode, doc, nid, true); 
        } catch (e) { log(e) }
    }

    function packTcn(tcn){
        if(!tcn.T) tcn.T=undefined;
        if(!tcn.C) tcn.C=undefined;
        if(!tcn.d) tcn.d=undefined;
        if(tcn.N) tcn.N=undefined;
        if(tcn.note) tcn.note=undefined;
        return JSON.stringify(tcn);
    }

    var waiting, waitCnt, waitRoot;
    var jsonNames={
		T:"typ"
        , C:"clr"
        , N:"note"
        , P:"ntyp"
        , B:"bon"
        , b:"bin"
        , Q:"bclr"
        , q:"fclr"
        , s:"fsz"
        , p:"tPos"
        //, d:"b1"
        , k:"check"
    },jsonNames1={};
    for(var k in jsonNames){
        jsonNames1[jsonNames[k]] = k;
    }
    
    // 将存储名扩展为可读名
    function upackRow(row){
        var ret = {},keys=Object.keys(row),i=0,k,v,k1;
        for(;k=keys[i++];) {
            v=row[k];
            k1=jsonNames[k];
            if(k1) k = k1;
            ret[k] = v;
        }
        return ret;
    }
    
    // 将可读名编码为存储名
    function packRow(row){
        var ret = {},keys=Object.keys(row),i=0,k,v,k1;
        for(;k=keys[i++];) {
            v=row[k];
            k1=jsonNames1[k];
            if(k1) k = k1;
            ret[k] = v;
        }
        return ret;
    }

    function restoreMarks(t, rootNode, doc) {
        log('fatal debug annot::restoreMarks::', t);
        waiting = 0;
        if(t.length) {
            t = t.split('\0');
            for(var i=0,len=t.length-2;i<len;i+=3) {
                try{
                    var tcn = JSON.parse(t[i]), nid = parseInt(t[i+1])
                        , nn = tcn.n.split(';'), n0=nn[0], n1=nn[1];
                    tcn = upackRow(tcn)
                    if(t[i+2])
                        tcn.note = t[i+2];
                    tcn.nid = nid;
                    if(nn.length==3) {n0=nn[0]+n1; n1=nn[0]+nn[2]}
                    else if(nn.length!=2) continue;
                    var range = makeRange(n0, n1, rootNode, doc, tcn);
                    log('restoring --- ', range?"sucess":"fail", nid);
                    if(range) {
                        var el = annot(tcn, -1, 0, 0);
                        wrapRange(range, el, rootNode, doc, nid)
                    } else {
                        if(waiting===0) waiting = [];
                        tcn.n = [n0,n1];
                        waiting.push(tcn);
                    }
                } catch(e) {log('error::', t[i], e)}
            }
            if(waiting) {
                waitCnt=0;
                waitRoot = rootNode;
                log('waiting=', waiting)
                setTimeout(relayMarks, 500)
            }
        }
    }

    function relayMarks() {
        var t=waiting,i=0,len=t.length,nw=0, hasNxt = ++waitCnt<3;
        for(;i<len;i++) {
            var tcn = t[i];
            try{
                var range = makeRange(tcn.n[0], tcn.n[1], waitRoot, doc, tcn);
                //range = 0;
                //log('retrying::'+tcn.n, range);
                if(range) {
                    var el = annot(tcn, -1, 0, 0);
                    wrapRange(range, el, waitRoot, doc, tcn.nid, tcn.check)
                } else {
                    if(nw===0) nw = [];
                    nw.push(tcn);
                }
                log(waitCnt, 'retry --- '+(range?'success':'fail'), tcn.nid)
            } catch(e) {log('error::', tcn, e)}
        }
        t=waiting=nw;
        if(t.length) {
            if(hasNxt) setTimeout(relayMarks, waitCnt>2?2500:waitCnt>1?1500:750)
            // else ...
        }
    }

    window.MakeMark=annot;
    window.MakeRange=makeRange;
    window.WrapRange=wrapRange;
    window.NidsInRange=getNidsInRange;
    window.PatchNote=patchNote;
    window.RestoreMarks=restoreMarks;
})();