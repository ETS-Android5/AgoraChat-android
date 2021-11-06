package io.agora.chatdemo.me;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import io.agora.chat.uikit.adapter.EaseBaseRecyclerViewAdapter;
import io.agora.chatdemo.R;
import io.agora.chatdemo.base.BaseListFragment;
import io.agora.chatdemo.databinding.FragmentUserAvaterSelectBinding;
import io.agora.chatdemo.general.utils.UIUtils;


public class UserAvatarSelectFragment extends BaseListFragment<Integer> implements SwipeRefreshLayout.OnRefreshListener {

    private List<Integer> avaterImages;
    private UserAvatarSelectViewModel mViewModel;
    private FragmentUserAvaterSelectBinding mBinding;
    private GridLayoutManager gridLayoutManager;

    @Override
    protected void initArgument() {
        super.initArgument();
        gridLayoutManager = new GridLayoutManager(mContext, 2);
        gridLayoutManager.setOrientation(RecyclerView.VERTICAL);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        mRecyclerView.addItemDecoration(new AvaterSelectItemDecoration(UIUtils.dp2px(mContext,10)));

    }

    @Override
    protected void initViewModel() {
        super.initViewModel();
        mViewModel = new ViewModelProvider(this).get(UserAvatarSelectViewModel.class);
    }

    @Override
    protected View getContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentUserAvaterSelectBinding.inflate(inflater);
        return mBinding.getRoot();
    }

    @Override
    protected RecyclerView initRecyclerView() {
        return mBinding.avatarList;
    }

    @Override
    protected void initData() {
        super.initData();
        avaterImages=new ArrayList<>();
        avaterImages.add(R.drawable.avatar_1);
        avaterImages.add(R.drawable.avatar_2);
        avaterImages.add(R.drawable.avatar_3);
        avaterImages.add(R.drawable.avatar_4);
        avaterImages.add(R.drawable.avatar_5);
        avaterImages.add(R.drawable.avatar_6);
        avaterImages.add(R.drawable.avatar_7);
        mListAdapter.setData(avaterImages);
    }

    @Override
    protected void initListener() {
        super.initListener();
        mBinding.srlContactRefresh.setOnRefreshListener(this);
    }

    @Override
    protected RecyclerView.LayoutManager getLayoutManager() {
        return gridLayoutManager;
    }

    @Override
    protected EaseBaseRecyclerViewAdapter<Integer> initAdapter() {
        return new AvaterSelectAdapter();
    }

    @Override
    public void onItemClick(View view, int position) {

    }

    @Override
    public void onRefresh() {
        //just it temp
        mListAdapter.setData(avaterImages);
        mBinding.srlContactRefresh.setEnabled(false);
    }
}