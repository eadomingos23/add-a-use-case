
<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span>Student Management</span>
      </div>
    </template>

    <!-- Search Form -->
    <el-form inline>
      <el-form-item label="Student First Name:">
        <el-input v-model="searchCriteria.firstName" @input="loadStudents" />
      </el-form-item>
      <el-form-item label="Student Last Name:">
        <el-input v-model="searchCriteria.lastName" @input="loadStudents" />
      </el-form-item>
      <el-form-item>
        <el-button @click="resetSearchCriteria">Reset</el-button>
      </el-form-item>
    </el-form>

    <!-- Student Table -->
    <el-table
        :data="students"
        style="width: 100%"
        stripe
        border
        v-loading="loading"
        @row-click="goToStudentPerformanceDashboard"
        height="600"
    >
      <el-table-column label="Id" prop="id"></el-table-column>
      <el-table-column label="First Name" prop="firstName"></el-table-column>
      <el-table-column label="Last Name" prop="lastName"></el-table-column>
      <el-table-column label="Email" prop="email"></el-table-column>
      <el-table-column label="Team" prop="teamName"></el-table-column>

      <el-table-column label="Status" prop="enabled">
        <template #default="{ row }">
          <el-tag type="success" v-if="row.enabled">Enabled</el-tag>
          <el-tag type="danger" v-else>Disabled</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="Operations" width="230">
        <template #default="{ row }">
          <el-button
              icon="Edit"
              circle
              plain
              type="primary"
              @click.stop="showEditStudentDialog(row)"
          ></el-button>

          <el-button
              v-if="row.enabled"
              type="danger"
              plain
              size="small"
              :loading="deactivatingStudentId === row.id"
              @click.stop="confirmDeactivateStudent(row)"
          >
            Deactivate
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="No data is available." />
      </template>
    </el-table>

    <!-- Pagination -->
    <el-pagination
        v-model:current-page="pageNumber"
        v-model:page-size="pageSize"
        :page-sizes="[10, 50, 100]"
        layout="jumper, total, sizes, prev, pager, next"
        background
        :total="totalElements"
        @size-change="handlePageSizeChange"
        @current-change="handlePageNumberChange"
        style="margin-top: 20px; justify-content: flex-end"
    />

    <!-- Dialog for editing student -->
    <el-dialog
        v-model="dialogVisible"
        title="Edit a student"
        width="30%"
        destroy-on-close
    >
      <EditStudentForm
          :studentId="studentBeingEditedId"
          @close-dialog="closeDialog"
      ></EditStudentForm>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { searchStudents, deactivateStudent } from '@/apis/student'
import type {
  SearchStudentByCriteriaResponse,
  Student,
  StudentSearchCriteria
} from '@/apis/student/types'
import { useSettingsStore } from '@/stores/settings'
import EditStudentForm from './EditStudentForm.vue'

const settingsStore = useSettingsStore()
const router = useRouter()

const searchCriteria = ref<StudentSearchCriteria>({
  sectionId: NaN,
  firstName: '',
  lastName: ''
})

const students = ref<Student[]>([])
const loading = ref<boolean>(true)
const deactivatingStudentId = ref<number | null>(null)

// Pagination information
const pageNumber = ref<number>(1)
const pageSize = ref<number>(50)
const totalElements = ref<number>(60)

// Load data when the component is mounted
onMounted(() => {
  searchCriteria.value.sectionId = settingsStore.defaultSectionId

  if (Number.isNaN(searchCriteria.value.sectionId)) {
    loading.value = false
    ElMessage.error('Please select a section in the Sections page.')
    return
  }

  loadStudents()
})

async function loadStudents() {
  loading.value = true

  try {
    const result: SearchStudentByCriteriaResponse = await searchStudents(
        { page: pageNumber.value - 1, size: pageSize.value },
        searchCriteria.value
    )

    students.value = result.data.content
    pageNumber.value = result.data.number + 1
    pageSize.value = result.data.size
    totalElements.value = result.data.totalElements
  } catch (error) {
    console.error('Failed to load students:', error)
    ElMessage.error('Unable to load students.')
  } finally {
    loading.value = false
  }
}

function resetSearchCriteria() {
  searchCriteria.value = {
    sectionId: settingsStore.defaultSectionId,
    firstName: '',
    lastName: ''
  }

  loadStudents()
}

function handlePageNumberChange(newPageNumber: number) {
  pageNumber.value = newPageNumber
  loadStudents()
}

function handlePageSizeChange(newSize: number) {
  pageSize.value = newSize
  pageNumber.value = 1
  loadStudents()
}

// Edit student dialog
const dialogVisible = ref(false)
const studentBeingEditedId = ref<number>()

function showEditStudentDialog(student: Student) {
  dialogVisible.value = true
  studentBeingEditedId.value = student.id as number
}

function closeDialog() {
  dialogVisible.value = false
  loadStudents()
}

// Confirm before deactivating a student
async function confirmDeactivateStudent(student: Student) {
  if (student.id == null || !student.enabled) {
    return
  }

  try {
    await ElMessageBox.confirm(
        `Are you sure you want to deactivate ${student.firstName} ${student.lastName}? The student will lose access to the application, but their existing academic records will be preserved.`,
        'Deactivate Student',
        {
          confirmButtonText: 'Deactivate',
          cancelButtonText: 'Cancel',
          confirmButtonClass: 'el-button--danger',
          type: 'warning'
        }
    )
  } catch {
    // Cancellation: do not send a deactivation request.
    return
  }

  deactivatingStudentId.value = student.id

  try {
    await deactivateStudent(student.id)
    ElMessage.success('Student deactivated successfully.')
    await loadStudents()
  } catch (error) {
    console.error('Failed to deactivate student:', error)
    ElMessage.error('Unable to deactivate student.')
  } finally {
    deactivatingStudentId.value = null
  }
}

function goToStudentPerformanceDashboard(student: Student) {
  router.push({
    name: 'student-performance-dashboard',
    params: { studentId: student.id }
  })
}
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100%;
  box-sizing: border-box;

  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}
</style>
