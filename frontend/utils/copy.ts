export function copyTextToClipboard(text: string) {
    if (navigator.clipboard && window.isSecureContext) {
      return navigator.clipboard.writeText(text).then(
        function () {
          console.log('Copying to clipboard was successful!');
          return true;
        },
        function (err) {
          console.error('Could not copy text: ', err);
          return false;
        }
      );
    } else {
      return new Promise(function (resolve, reject) {
        var textArea = document.createElement('textarea');
        Object.assign(textArea.style, {
          position: 'fixed',
          top: 0,
          left: 0,
          width: '2em',
          height: '2em',
          padding: 0,
          border: 'none',
          outline: 'none',
          boxShadow: 'none',
          background: 'transparent'
        });
  
        textArea.value = text;
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
  
        try {
          var successful = document.execCommand('copy');
          document.body.removeChild(textArea);
          if (successful) {
            console.log('Copying text command was successful');
            resolve(true);
          } else {
            console.error('Copying text command was unsuccessful');
            resolve(false);
          }
        } catch (err) {
          console.log('Oops, unable to copy', err);
          reject(err);
        }
      });
    }
  }