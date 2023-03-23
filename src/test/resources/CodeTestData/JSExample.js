
var file;

function run()
{
	getFile();
}

function getFile()
{
	return async function()
	{
		var input = document.createElement('input');
		input.type = 'file';		
		input.onchange = e => { 
		   file = e.target.files[0]; 
		   submitResult();
		}		
		input.click();
	}
}

async function submitResult()
{
	// we need to make sure that the file upload is done before submitting the other results.
	await window.uploadFile(file.name, file, "myFirstFileResult");
	window.reportResults({"taskFinished" : true})						

}