// resolve entry:// link dynamically in mdict.js      
//      // rewrite in-page link
//      $content.find('a[href^="entry://"]').each(function() { 
//        var $el = $(this), href = $el.attr('href');
//        if (href.match('#')) {
//          $el.attr('href', href.substring(8));
//        }
//      });
      
	  
	  
	      // jump to word with link started with "entry://"
    // TODO: have to ignore in-page jump
    $('#definition').on('click', 'a', function(e) {
      var href = $(this).attr('href');
      if (href && href.substring(0, 8) === 'entry://') {
        var word = href.substring(8);
        // TODO: remove '#' to get jump target
        if (word.charAt(0) !== '#') {
          word = word.replace(/(^[/\\])|([/]$)/, '');

          $('#word').val(word);
          $('#btnLookup').click();
        } else {
          var currentUrl = location.href;
          location.href = word;                       //Go to the target element.
          history.replaceState(null,null,currentUrl); //Don't like hashes. Changing it back.        
        }
      }
    });
  }
