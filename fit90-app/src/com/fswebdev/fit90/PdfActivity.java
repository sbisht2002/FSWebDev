package com.fswebdev.fit90;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.fswebdev.fit90.content.Content;
import com.fswebdev.fit90.data.ShareProvider;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** Lists the free PDF guides and shows one, with save-to-device and share. */
public class PdfActivity extends BaseActivity {

    public static final String EXTRA_ID = "id";
    private static final int REQ_SAVE = 3;
    private File current;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String id = getIntent().getStringExtra(EXTRA_ID);
        if (id == null) list();
        else show(id);
    }

    private void list() {
        screen(getString(R.string.pdf_guides), true);
        content.addView(ui.text(getString(R.string.pdf_intro), Ui.BODY, p.sub));
        content.addView(ui.space(12));
        JSONArray guides = Content.of(this).json("guides.json").optJSONArray("guides");
        for (int i = 0; guides != null && i < guides.length(); i++) {
            final JSONObject g = guides.optJSONObject(i);
            content.addView(ui.listRow(R.drawable.ic_doc, p.tone("gold"), g.optString("title"), g.optString("subtitle"),
                    null, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            open(PdfActivity.class, EXTRA_ID, g.optString("id"));
                        }
                    }));
        }
    }

    private File copy(String id) throws Exception {
        File out = new File(ShareProvider.dir(this), id + ".pdf");
        InputStream in = getAssets().open("pdf/" + id + ".pdf");
        OutputStream os = new FileOutputStream(out);
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        os.close();
        in.close();
        return out;
    }

    private void show(final String id) {
        screen("", true);
        try {
            current = copy(id);
        } catch (Exception e) {
            content.addView(empty(getString(R.string.not_found)));
            return;
        }
        action(R.drawable.ic_share, getString(R.string.share), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent send = new Intent(Intent.ACTION_SEND).setType("application/pdf")
                        .putExtra(Intent.EXTRA_STREAM, ShareProvider.uriFor(current))
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(send, getString(R.string.share)));
            }
        });
        action(R.drawable.ic_download, getString(R.string.save_to_device), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/pdf").putExtra(Intent.EXTRA_TITLE, id + ".pdf"), REQ_SAVE);
            }
        });
        try {
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(current, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer r = new PdfRenderer(fd);
            int width = getResources().getDisplayMetrics().widthPixels - ui.dp(40);
            for (int i = 0; i < r.getPageCount(); i++) {
                PdfRenderer.Page page = r.openPage(i);
                int h = (int) (width * (page.getHeight() / (float) page.getWidth()));
                Bitmap bmp = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888);
                bmp.eraseColor(Color.WHITE);
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                ImageView iv = new ImageView(this);
                iv.setImageBitmap(bmp);
                iv.setContentDescription(getString(R.string.page_n, i + 1));
                content.addView(iv, ui.margins(new LinearLayout.LayoutParams(width, h), 0, 0, 0, 12));
            }
            r.close();
            fd.close();
        } catch (Exception e) {
            content.addView(empty(getString(R.string.not_found)));
        }
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req != REQ_SAVE || result != RESULT_OK || data == null || current == null) return;
        try {
            OutputStream os = getContentResolver().openOutputStream(data.getData());
            InputStream in = new java.io.FileInputStream(current);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            in.close();
            os.close();
            toast(getString(R.string.saved));
        } catch (Exception e) {
            toast(getString(R.string.export_failed));
        }
    }
}
