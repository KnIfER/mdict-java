(function(){
	if(!window.tpshc){
		if(window.shzh==undefined) {
			window.shzh=app.rcsp(sid.get());
		}
		var debug = function(a,b,c,d,e){var t=[a,b,c,d,e];for(var i=5;i>=0;i--){if(t[i]===undefined)t[i]='';else break}console.log("%cTapSch","color:(#FFF);background:#00f;",t[0],t[1],t[2],t[3],t[4])}
		debug('popuping ini...shzh='+window.shzh);
		var tRange, tTime;
		// 0=word; 1=paragraph; 
		var granu;
		var bAutoGranu = true;
		var dx, dy;
		window.addEventListener('touchstart', function(e){
			var w=this;
			if(!w.getSelection().isCollapsed && !w._touchtarget_lck) {
				tRange = w.getSelection().getRangeAt(0);
				var t = e.changedTouches[0];
				dx = t.clientX;
				dy = t.clientY;
				tTime = e.timeStamp;
			} else if(tRange) {
				tRange = 0;
			}
		});
		window.addEventListener('touchend', function(e){
			//debug('popuping::touchend ini...');
			var w=this;
			if(tRange && bAutoGranu && granu>0 && !w._touchtarget_lck) {
				if(e.timeStamp-tTime>=350) return
				var t = e.changedTouches[0];
				dx=t.clientX-dx;
				dy=t.clientY-dy;
				// make sure that distance between touchstart and touchend smaller than some threshold,
				// e.g. <= 16 px
				if (dx*dx < 80 && dy*dy< 80) {
					var rs=tRange.getClientRects(),rc = rs[0], x=t.clientX, y=t.clientY;
					// debug('\n')
					// debug('边缘触发', x, y, x<=rc.x+50 , x>=rc.x , y>=rc.y , y<=rc.bottom)
					// debug('边缘触发', rc.x, rc.right , rc.y , rc.bottom)
					// debug('\n')
					var pad = 18;
					if(x<=rc.left+pad && x>=rc.left && y>=rc.top && y<=rc.bottom) {
						return;
					}
					rc = rs[rs.length-1];
					if(x<=rc.right && x>=rc.right-pad && y>=rc.top && y<=rc.bottom) {
						return;
					}
					debug('将触发点击事件，提前清空选择。', dx*dx, dy*dy)
					w.getSelection().empty();
				}
			}
		});
		function pointInRange(e, rc) {
			var x = rc.left;
			var y = rc.top;
			var w = parseInt(rc.width);
			var h = parseInt(rc.height);
			var pY = e.clientY;
			var pX = e.clientX;
			var pad=50;
			return w>0 && y>0
			&& (pY>y-pad && pY<y+h+pad && pX>x-10 && pX<x+w+10);
		}
		window.addEventListener('click',window.tpshc=function(e){
			var tar=e.target, w=this, pw=parent, d=w.document, sz=pw.shzh, app=pw.app,excluded=false;
			debug('popuping::click...设置=', sz, e, tar);
			//debug(sz&7 , curr!=d.documentElement , curr.nodeName , !curr.noword, !w._touchtarget_lck);
			if(sz&7 && tar!=d.documentElement && !tar.noword && !w._touchtarget_lck){
				var s = w.getSelection(), p=e.path;
				debug('来了',s.isCollapsed && s.anchorNode);
				if(!(s.isCollapsed && s.anchorNode)) {
					// don't bother with user selection
					return;
				}
				if(!p && e.composedPath) p=e.composedPath();
				if(p) for(var i=0,t,n;(t=p[i])&&i++<5;) {
					n=t.tagName;
					if(n==='A'&&(t.href||t.onclick)||n==='BUTTON') {tar=0;break;}
					else if(n==='TEXTAREA'||n==='INPUT') if(!t.readOnly) {tar=0;break;}
				}
				if(p) excluded = !tar;
				// merge _NWP & _YWPC
				if(tar && w._WORDCON) { // 排除界面元素
					var p=tar, stat=0, hasInc=0; 
					while(p) {
						for(var i=0,q;q=_WORDCON[i++];) {
							var exc = q[0]==='-';
							if(exc) q = q.slice(1);
							else hasInc = 1;
							if(p.matches(q)) {
								if(exc) {
									stat = -1;
								} else {
									stat = 1;
								}
								break;
							}
						}
						p=p.parentElement;
					}
					if(stat<0)
						tar=0;
					if(hasInc && stat!=1)
						tar=0;
					excluded = !tar;
				}
				if(tar) {
					var range = s.getRangeAt(0), rg, rc;

					// 获得落点处字符方框
					s.modify('extend', 'backward', 'character'); 
					rg = s.getRangeAt(0);
					rc = rg.getBoundingClientRect();
					// suppressTextMenu
					var tapSel = sz&6;
					if(!tapSel) app.ntxt = e.timeStamp;
					if(!(rc.left<=e.x && rc.right>=e.x)) {
						s.empty(); s.addRange(range);
						s.modify('extend', 'forward', 'character'); 
						rg = s.getRangeAt(0);
						rc = rg.getBoundingClientRect();
					}
					var ret = -1;
					if(pointInRange(e, rc)) {
						debug('fatal 单字='+rg, 'tapSel='+tapSel, 'granu='+granu);
						if(tapSel) {
							s.empty();s.addRange(rg);
							var nntd = '_pd_nntd'
							var sty = d.getElementById(nntd);
							if(!sty) {
								sty = d.createElement('STYLE');
								d.head.appendChild(sty);
								sty.id = nntd;
							}
							//sty.innerText = '._PDB.note{visibility:hidden}'; // skip note texts
							sty.innerText = '._PDB.note{visibility:hidden}'; // skip note texts

							s.modify('extend', 'backward', 'paragraphboundary');
							var textParag = s.toString(), now = textParag.length;
							s.collapseToStart();
							s.modify('extend', 'forward', 'paragraphboundary');
							
							if(tapSel==2) granu=0;
							else {
								if(bAutoGranu && tRange) {
									debug('检查包含点', granu)
									if(tRange.isPointInRange(rg.startContainer, rg.startOffset)) {
										debug('包含点!!!');
										if(granu==1) {
											granu = 0;
										}
									} else {
										granu = 1;
									}
								} else {
									granu = 1;
								}
							}

							if(granu==0) 
							{
								//console.time('probeWord')
								var sted = app.probeWord(sid.get(), s.toString(), textParag);
								//console.timeEnd('probeWord')

								var tst, ted=sted&0xFFFFFFFF;
								var num = (sted).toString(16);
								tst = parseInt('0x'+num.slice(0,num.length-8))||0;
								
								debug('wrappedClickFunc=', tst, ted, now);
								
								if(now>=tst && now<=ted) {
									s.empty();s.addRange(rg);
									s.collapseToStart();
									var r=s.getRangeAt(0);
									var st=r.startContainer,so=r.startOffset,ed=r.endContainer,eo=r.endOffset;
									//debug('r='+r, st, so, ed, eo);
									//debug('movebackward', now-tst);
									for(var i=0;i<now-tst;i++) {
										s.modify('extend', 'backward', 'character');
										r=s.getRangeAt(0);
										st=r.startContainer;so=r.startOffset;
									}
									s.empty();s.addRange(rg);
									s.collapseToStart();
									for(var i=0;i<ted-now;i++) {
										s.modify('extend', 'forward', 'character');
										r=s.getRangeAt(0);
										ed=r.endContainer;eo=r.endOffset;
									// ed=r.startContainer;eo=r.startOffset;
									}
									r = new Range();
									r.setStart(st, so);
									r.setEnd(ed, eo);
									s.empty();
									s.addRange(r);
								}
							}
							
							sty.innerText=''
							
							ret = undefined;
						}
						else {
							// 先简单取词 'word'
							s.empty(); s.addRange(rg);
							s.collapseToStart();
							s.modify('move', 'backward', 'word');
							s.modify('extend', 'forward', 'word');
							range = s.getRangeAt(0);
							if(!(range.comparePoint(rg.startContainer, rg.startOffset)==0
								&& range.comparePoint(rg.endContainer, rg.endOffset)==0)) {
								s.empty(); s.addRange(rg);
								s.collapseToStart();
								s.modify('extend', 'forward', 'word');
								range = s.getRangeAt(0);
							}
							
							var text = s.toString(), len=text.trim().length;
							if(len){
								// 测试大内存取词
								if(len==1 && app.hexie(text.charCodeAt(0))) {
									// 开始大内存取词
									s.empty();s.addRange(rg);
									// 先取整个段落
									s.modify('extend', 'backward', 'paragraphboundary');
									var textParag = s.toString(), now = textParag.length;
									s.collapseToStart();
									s.modify('extend', 'forward', 'paragraphboundary');
									// 再取词
									//console.time('probeWord')
									var sted = app.probeWord(sid.get(), s.toString(), textParag);
									//console.timeEnd('probeWord')
									var tst, ted=sted&0xFFFFFFFF;
									var num = (sted).toString(16);
									tst = parseInt('0x'+num.slice(0,num.length-8))||0;
									debug('wrappedClickFunc=', tst, ted, now);
									if(now>=tst && now<=ted) {
										s.empty();s.addRange(rg);
										s.collapseToStart();
										var r=s.getRangeAt(0);
										var st=r.startContainer,so=r.startOffset,ed=r.endContainer,eo=r.endOffset;
										//debug('r='+r, st, so, ed, eo);
										//debug('movebackward', now-tst);
										for(var i=0;i<now-tst;i++) {
											s.modify('extend', 'backward', 'character');
											r=s.getRangeAt(0);
											st=r.startContainer;so=r.startOffset;
										}
										s.empty();s.addRange(rg);
										s.collapseToStart();
										for(var i=0;i<ted-now;i++) {
											s.modify('extend', 'forward', 'character');
											r=s.getRangeAt(0);
											ed=r.endContainer;eo=r.endOffset;
										// ed=r.startContainer;eo=r.startOffset;
										}
										r = new Range();
										r.setStart(st, so);
										r.setEnd(ed, eo);
										s.empty();
										s.addRange(r);
										range = r;
										text = r.toString();
									}
								}
								rc = range.getBoundingClientRect();
								app.popupWord(sid.get(), text, 0/*frameAt */, d.documentElement.scrollLeft+rc.left, d.documentElement.scrollTop+rc.top, rc.width, rc.height);
								w.popup=1;
								//s.empty();
								ret = true;
							}
						}
						if(!s.isCollapsed) {
							rg = s.getRangeAt(0);
							var t=pw.abSel, f=(pw.abSeI||0)+1;
							if(!t || t[2]!=s) {
								pw.abSel=t=[];
								t[2] = s;
							}
							t[pw.abSeI=f%2] = rg;
						}
						if(ret!=-1) return ret;
					}
				}
				//点击空白关闭点译弹窗
				//...
				if(!excluded) s.empty();
			}
			if(w.popup){
				app.popupClose(sid.get());
				w.popup=0;
			}
		});
	}
})()