$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide"); //点 发布 的时候把刚才填帖子内容的对话框隐藏掉
	// 获取标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 向服务器发送异步请求（POST）
	$.post(
		CONTEXT_PATH+"/discuss/add",
		{
			"title": title,
			"content": content,
		},
		function(data){
			data = $.parseJSON(data);
			//在提示框中显示返回消息
			$("#hintBody").text(data.msg);
			$("#hintModal").modal("show"); //显示提示框
			// 2秒后，自动隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				//如果发布成功自动刷新页面
				if(data.code==0){
					window.location.reload();
				}
			}, 2000);
		}
	);


}