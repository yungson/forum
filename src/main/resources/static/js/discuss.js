function like(button, entityType, entityId, entityUserId, postId){
    $.post(
        CONTEXT_PATH+"/like",
        {"entityType": entityType, "entityId": entityId, "entityUserId": entityUserId, "postId": postId},
        function(data){
            data = $.parseJSON(data);
            if(data.code==0){
                $(button).children("i").text(data.likeCount);
                $(button).children("b").text(data.likeStatus==1?"已赞":"赞");
            }else{
                alert(data.msg);
            }
        }
    );
}