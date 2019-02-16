package org.nustaq.kontraktor.apputil;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import java.awt.*;

public interface ImageHandlingSessionMixin {

    default IPromise<String> uploadImage(String base64String , String imageType ){
        Promise<String> res = new Promise();
        Actors.exec.execute( ()-> getImageSaver().handleImage(base64String,imageType,"user", new Dimension(512,512)).then( res ) );
        return res;
    }

    default ImageSaver getImageSaver() {
        return new ImageSaver("./run/upload/image");
    }

}
