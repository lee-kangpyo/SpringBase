package com.akmz.springBase.interFace.mapper;

import com.akmz.springBase.interFace.model.entity.Attach;
import com.akmz.springBase.interFace.model.entity.AttachFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import org.springframework.data.domain.Sort;

import java.util.List;

@Mapper
public interface AttachMapper {

    /**
     * 새로운 첨부 묶음을 생성합니다.
     * @param attach 생성할 첨부 정보
     */
    void insertAttach(Attach attach);

    /**
     * 개별 첨부파일 정보를 저장합니다.
     * @param attachFile 저장할 파일 정보
     */
    void insertAttachFile(AttachFile attachFile);

    /**
     * 특정 첨부 ID에 속한 모든 파일 목록을 조회합니다. (삭제된 파일 제외)
     * @param attachId 조회할 첨부 ID
     * @return 파일 목록
     */
    List<AttachFile> findFilesByAttachId(@Param("attachId") Long attachId);

    /**
     * 파일 ID로 특정 파일의 정보를 조회합니다.
     * @param fileId 조회할 파일 ID
     * @return 파일 정보
     */
    AttachFile findFileById(@Param("fileId") Long fileId);

    /**
     * 파일을 논리적으로 삭제 처리합니다. (상태를 'DELETED'로 변경)
     * @param fileId 삭제할 파일 ID
     */
    void softDeleteFileById(@Param("fileId") Long fileId);

    /**
     * 특정 첨부 ID에 속한 모든 파일을 논리적으로 삭제 처리합니다. (상태를 'DELETED'로 변경)
     * @param attachId 삭제할 첨부 ID
     */
    void softDeleteFilesByAttachId(@Param("attachId") Long attachId);

    /**
     * 모든 첨부파일 목록을 페이징하여 조회합니다. (삭제된 파일 제외)
     * @param offset 조회 시작 오프셋
     * @param limit 조회할 레코드 수
     * @param sort 정렬 정보
     * @return 첨부파일 목록
     */
    List<AttachFile> findAllAttachFiles();

    /**
     * 모든 첨부 묶음 목록을 페이징하여 조회합니다.
     * @return 첨부 묶음 목록
     */
    List<Attach> findAllAttaches();

    /**
     * 모든 첨부파일의 총 개수를 조회합니다. (삭제된 파일 제외)
     * @return 총 첨부파일 개수
     */

}
