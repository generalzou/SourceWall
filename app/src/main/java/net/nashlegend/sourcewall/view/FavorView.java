package net.nashlegend.sourcewall.view;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import net.nashlegend.sourcewall.R;
import net.nashlegend.sourcewall.adapters.FavorAdapter;
import net.nashlegend.sourcewall.commonview.AAsyncTask;
import net.nashlegend.sourcewall.commonview.IStackedAsyncTaskInterface;
import net.nashlegend.sourcewall.model.AceModel;
import net.nashlegend.sourcewall.model.Article;
import net.nashlegend.sourcewall.model.Basket;
import net.nashlegend.sourcewall.model.Category;
import net.nashlegend.sourcewall.model.Post;
import net.nashlegend.sourcewall.model.Question;
import net.nashlegend.sourcewall.request.ResultObject;
import net.nashlegend.sourcewall.request.api.UserAPI;
import net.nashlegend.sourcewall.util.ToastUtil;

import java.util.ArrayList;

/**
 * Created by NashLegend on 2014/12/2 0002
 */
public class FavorView extends FrameLayout implements View.OnClickListener, IStackedAsyncTaskInterface {

    ListView listView;
    ProgressBar progressBaskets;
    FavorAdapter adapter;
    Button btn_invoke_create;
    Button btn_ok;
    Button btn_create_basket;
    Button btn_cancel_create_basket;
    Spinner spinner;
    ViewGroup listLayout;
    ViewGroup editLayout;
    TextView nameText;
    TextView introText;
    ArrayList<Category> categories;

    public FavorView(Context context) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.dialog_favor, this);
        nameText = (TextView) findViewById(R.id.text_create_basket_name);
        introText = (TextView) findViewById(R.id.text_create_basket_introduction);
        listLayout = (ViewGroup) findViewById(R.id.layout_favor_dialog_list);
        editLayout = (ViewGroup) findViewById(R.id.layout_favor_dialog_edit);
        btn_invoke_create = (Button) findViewById(R.id.button_favor_dialog_invoke_create);
        btn_ok = (Button) findViewById(R.id.button_favor_dialog_ok);
        btn_cancel_create_basket = (Button) findViewById(R.id.button_cancel_create_basket);
        btn_create_basket = (Button) findViewById(R.id.button_create_basket);
        spinner = (Spinner) findViewById(R.id.spinner_categories);
        listView = (ListView) findViewById(R.id.list_favor_dialog);
        progressBaskets = (ProgressBar) findViewById(R.id.progress_loading_baskets);
    }

    public void setData(AceModel model) {
        String link = "";
        String title = "";
        if (model instanceof Article) {
            link = "http://www.guokr.com/article/" + ((Article) model).getId() + "/";
            title = ((Article) model).getTitle();
        } else if (model instanceof Post) {
            link = "http://www.guokr.com/post/" + ((Post) model).getId() + "/";
            title = ((Post) model).getTitle();
        } else {
            link = "http://www.guokr.com/question/" + ((Question) model).getId() + "/";
            title = ((Question) model).getTitle();
        }
        adapter = new FavorAdapter(getContext(), link, title);
        listView.setAdapter(adapter);
        btn_invoke_create.setOnClickListener(this);
        btn_ok.setOnClickListener(this);
        btn_cancel_create_basket.setOnClickListener(this);
        btn_create_basket.setOnClickListener(this);
        loadBasket();
    }

    private void loadBasket() {
        LoadBasketTask task = new LoadBasketTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void createBasket(String title, String introduction, String category_id) {
        CreateBasketTask task = new CreateBasketTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, title, introduction, category_id);
    }

    private void loadCategories() {
        LoadCategoryTask task = new LoadCategoryTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void openCreateBasketView() {
        editLayout.setVisibility(VISIBLE);
        listLayout.setVisibility(GONE);
        if (categories == null) {
            loadCategories();
        }
    }

    private void openBasketListView() {
        listLayout.setVisibility(VISIBLE);
        editLayout.setVisibility(GONE);
        nameText.setText("");
        introText.setText("");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_favor_dialog_invoke_create:
                openCreateBasketView();
                break;
            case R.id.button_favor_dialog_ok:
                break;
            case R.id.button_cancel_create_basket:
                openBasketListView();
                break;
            case R.id.button_create_basket:
                String cateID = "-1";
                if (categories != null && categories.size() > 0 && spinner.getSelectedItemPosition() >= 0) {
                    cateID = String.valueOf(categories.get(spinner.getSelectedItemPosition()).getId());
                }
                createBasket(nameText.getText().toString(), introText.getText().toString(), cateID);
                break;
        }
    }

    private final ArrayList<AAsyncTask> stackedTasks = new ArrayList<>();

    @Override
    public void addToStackedTasks(AAsyncTask task) {
        stackedTasks.add(task);
    }

    @Override
    public void removeFromStackedTasks(AAsyncTask task) {
        stackedTasks.remove(task);
    }

    @Override
    public void flushAllTasks() {
        stackedTasks.clear();
    }

    @Override
    public void stopAllTasks() {
        for (int i = 0; i < stackedTasks.size(); i++) {
            AAsyncTask task = stackedTasks.get(i);
            if (task != null && task.getStatus() == AAsyncTask.Status.RUNNING) {
                task.cancel(true);
            }
        }
        stackedTasks.clear();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAllTasks();
        super.onDetachedFromWindow();
    }

    class LoadBasketTask extends AsyncTask<Void, Integer, ResultObject> {

        @Override
        protected void onPreExecute() {
            progressBaskets.setVisibility(VISIBLE);
            listView.setVisibility(INVISIBLE);
        }

        @Override
        protected ResultObject doInBackground(Void... params) {
            return UserAPI.getBaskets();
        }

        @Override
        protected void onPostExecute(ResultObject resultObject) {
            if (resultObject.ok) {
                ArrayList<Basket> baskets = (ArrayList<Basket>) resultObject.result;
                adapter.clear();
                if (baskets != null && baskets.size() > 0) {
                    adapter.setList(baskets);
                }
                adapter.notifyDataSetChanged();
            } else {
                ToastUtil.toast("加载果篮失败");
            }
            progressBaskets.setVisibility(INVISIBLE);
            listView.setVisibility(VISIBLE);
        }
    }

    class LoadCategoryTask extends AsyncTask<Void, Integer, ResultObject> {

        @Override
        protected ResultObject doInBackground(Void... params) {
            return UserAPI.getCategoryList();
        }

        @Override
        protected void onPostExecute(ResultObject resultObject) {
            if (resultObject.ok) {
                categories = (ArrayList<Category>) resultObject.result;
                String[] items = new String[categories.size()];
                for (int i = 0; i < categories.size(); i++) {
                    items[i] = categories.get(i).getName();
                }
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), R.layout.simple_spinner_item, items);
                spinner.setAdapter(arrayAdapter);
            } else {
                ToastUtil.toast("加载目录失败");
            }
        }
    }


    class CreateBasketTask extends AsyncTask<String, Integer, ResultObject> {

        @Override
        protected ResultObject doInBackground(String... params) {
            String title = params[0];
            String introduction = params[1];
            String categoryID = params[2];
            return UserAPI.createBasket(title, introduction, categoryID);
        }

        @Override
        protected void onPostExecute(ResultObject resultObject) {
            if (resultObject.ok) {
                ToastUtil.toast("创建成功");
                Basket basket = (Basket) resultObject.result;
                adapter.add(basket);
                adapter.notifyDataSetChanged();
                openBasketListView();
            } else {
                ToastUtil.toast("创建失败");
            }
        }
    }
}
