package io.agora.chatdemo.group.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

import io.agora.chat.ChatClient;
import io.agora.chat.Group;
import io.agora.chat.uikit.EaseUIKit;
import io.agora.chat.uikit.models.EaseConvSet;
import io.agora.chat.uikit.provider.EaseConversationInfoProvider;
import io.agora.chat.uikit.widget.EaseTitleBar;
import io.agora.chatdemo.R;
import io.agora.chatdemo.base.BaseInitActivity;
import io.agora.chatdemo.databinding.ActivityGroupDetailBinding;
import io.agora.chatdemo.general.callbacks.OnResourceParseCallback;
import io.agora.chatdemo.general.constant.DemoConstant;
import io.agora.chatdemo.general.dialog.SimpleDialog;
import io.agora.chatdemo.general.livedatas.EaseEvent;
import io.agora.chatdemo.general.livedatas.LiveDataBus;
import io.agora.chatdemo.group.DisbandGroupDialog;
import io.agora.chatdemo.group.GroupHelper;
import io.agora.chatdemo.group.viewmodel.GroupDetailViewModel;

public class GroupDetailActivity extends BaseInitActivity implements View.OnClickListener {

    private String groupId;
    private Group group;
    protected ActivityGroupDetailBinding binding;
    private GroupDetailViewModel viewModel;
    private boolean fromChat;

    public static void actionStart(Context context, String groupId) {
        actionStart(context, groupId, false);
    }

    public static void actionStart(Context context, String groupId, boolean isFromChat) {
        Intent intent = new Intent(context, GroupDetailActivity.class);
        intent.putExtra("group_id", groupId);
        intent.putExtra("from_chat", isFromChat);
        context.startActivity(intent);
    }

    @Override
    protected View getContentView() {
        binding = ActivityGroupDetailBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected void initIntent(Intent intent) {
        super.initIntent(intent);
        groupId = getIntent().getStringExtra("group_id");
        fromChat = getIntent().getBooleanExtra("from_chat", false);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        group = ChatClient.getInstance().groupManager().getGroup(groupId);
        binding.itemGroupTransfer.setVisibility(View.GONE);
        binding.itemDisbandGroup.setVisibility(View.GONE);
        binding.itemLeaveGroup.setVisibility(View.GONE);
        if(fromChat) {
            binding.includeInfo.ivChat.setVisibility(View.GONE);
            binding.includeInfo.tvChat.setVisibility(View.GONE);
        }
        setGroupView();
    }

    @Override
    protected void initListener() {
        super.initListener();
        binding.includeInfo.getRoot().setOnClickListener(this);
        binding.itemGroupMembers.setOnClickListener(this);
        binding.itemGroupNotice.setOnClickListener(this);
        binding.itemGroupFiles.setOnClickListener(this);
        binding.itemGroupTransfer.setOnClickListener(this);
        binding.itemLeaveGroup.setOnClickListener(this);
        binding.itemDisbandGroup.setOnClickListener(this);
        binding.toolbarGroupDetail.setOnBackPressListener(new EaseTitleBar.OnBackPressListener() {
            @Override
            public void onBackPress(View view) {
                onBackPressed();
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.include_info:
                showEditDialog();
                break;
            case R.id.item_group_members:
                skipToMemberList();
                break;
            case R.id.item_group_notice:
                skipToNotice();
                break;
            case R.id.item_group_files:
                skipToFiles();
            case R.id.item_group_transfer:
                skipToTransfer();
                break;
            case R.id.item_leave_group:
                leaveGroup();
                break;
            case R.id.item_disband_group:
                disbandGroup();
                break;
        }
    }

    @Override
    protected void initData() {
        super.initData();
        viewModel = new ViewModelProvider(this).get(GroupDetailViewModel.class);
        viewModel.getAnnouncementObservable().observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<String>() {
                @Override
                public void onSuccess(@Nullable String data) {
                    binding.tvNotice.setText(data);
                }
            });
        });
        viewModel.getGroupObservable().observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<Group>() {
                @Override
                public void onSuccess(@Nullable Group data) {
                    group = data;
                    setGroupView();
                }
            });
        });
        viewModel.getRefreshObservable().observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<String>() {
                @Override
                public void onSuccess(@Nullable String data) {
                    loadGroup();
                }
            });
        });
        viewModel.getLeaveGroupObservable().observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<Boolean>() {
                @Override
                public void onSuccess(@Nullable Boolean data) {
                    finish();
                    LiveDataBus.get().with(DemoConstant.GROUP_CHANGE).postValue(EaseEvent.create(DemoConstant.GROUP_LEAVE, EaseEvent.TYPE.GROUP, groupId));
                }
            });
        });
        LiveDataBus.get().with(DemoConstant.GROUP_CHANGE, EaseEvent.class).observe(this, event -> {
            if(event.isGroupLeave() && TextUtils.equals(groupId, event.message)) {
                finish();
                return;
            }
            if(event.isGroupChange()) {
                loadGroup();
            }
        });
        loadGroup();
    }

    private void loadGroup() {
        viewModel.getGroup(groupId);
        viewModel.getGroupAnnouncement(groupId);
    }

    private void setGroupView() {
        if(group == null) {
            return;
        }
        if(GroupHelper.isOwner(group)) {
            binding.itemGroupTransfer.setVisibility(View.VISIBLE);
            binding.itemDisbandGroup.setVisibility(View.VISIBLE);
            binding.itemLeaveGroup.setVisibility(View.GONE);
        }else if(GroupHelper.isAdmin(group)) {
            binding.itemGroupTransfer.setVisibility(View.GONE);
            binding.itemLeaveGroup.setVisibility(View.VISIBLE);
            binding.itemDisbandGroup.setVisibility(View.GONE);
        }else {
            binding.itemGroupTransfer.setVisibility(View.GONE);
            binding.itemLeaveGroup.setVisibility(View.VISIBLE);
            binding.itemDisbandGroup.setVisibility(View.GONE);
        }

        EaseConversationInfoProvider conversationInfoProvider = EaseUIKit.getInstance().getConversationInfoProvider();
        if(conversationInfoProvider != null) {
            EaseConvSet info = conversationInfoProvider.getConversationInfo(groupId);
            if(info != null) {
                String title = "";
                if(!TextUtils.isEmpty(info.getName())) {
                    title = info.getName();
                }
                Drawable icon = info.getIcon();
                Glide.with(this)
                        .load(info.getIconUrl())
                        .placeholder(R.drawable.icon)
                        .error(icon != null ? icon : R.drawable.icon)
                        .into(binding.includeInfo.ivAvatar);
                binding.includeInfo.tvName.setText(title);
            }else {
                setGroupInfo();
            }
        }else {
            setGroupInfo();
        }
        binding.includeInfo.tvId.setText(getString(R.string.show_agora_chat_id, groupId));
        binding.includeInfo.tvDescription.setText(group.getDescription());
        binding.itemGroupMembers.setContent(String.valueOf(group.getMemberCount()));
    }

    private void setGroupInfo() {
        String title = GroupHelper.getGroupName(groupId);
        binding.includeInfo.tvName.setText(title);
        binding.includeInfo.ivAvatar.setImageResource(R.drawable.icon);
    }

    private void showEditDialog() {
        if(GroupHelper.isOwner(group) || GroupHelper.isAdmin(group)) {
            //
        }
    }

    protected void skipToMemberList() {

    }

    private void skipToNotice() {

    }

    private void skipToFiles() {

    }

    private void skipToTransfer() {
        if(GroupHelper.isOwner(group)) {
            //
        }
    }

    private void leaveGroup() {
        new SimpleDialog.Builder(mContext)
                .setTitle(R.string.group_detail_leave_hint_title)
                .setContent(R.string.group_detail_leave_hint_content)
                .setOnConfirmClickListener(R.string.group_detail_leave_hint_confirm, new SimpleDialog.OnConfirmClickListener() {
                    @Override
                    public void onConfirmClick(View view) {
                        viewModel.leaveGroup(groupId);
                    }
                })
                .setConfirmColor(R.color.color_alert)
                .showCancelButton(true)
                .show();
    }

    private void disbandGroup() {
        new DisbandGroupDialog.Builder(mContext)
                .setOnTransferClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // skip to transfer
                        skipToTransfer();
                    }
                })
                .setTitle(R.string.group_detail_disband_hint_title)
                .setContent(R.string.group_detail_disband_hint_content)
                .setOnConfirmClickListener(new SimpleDialog.OnConfirmClickListener() {
                    @Override
                    public void onConfirmClick(View view) {
                        // disband group
                        viewModel.destroyGroup(groupId);
                    }
                })
                .setOnCancelClickListener(new SimpleDialog.onCancelClickListener() {
                    @Override
                    public void onCancelClick(View view) {
                        // do nothing
                    }
                })
                .show();
    }
}